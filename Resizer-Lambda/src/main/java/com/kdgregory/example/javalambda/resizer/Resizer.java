// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.resizer;


import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;

import com.kdgregory.example.javalambda.shared.config.Environment;
import com.kdgregory.example.javalambda.shared.services.content.ContentService;
import com.kdgregory.example.javalambda.shared.services.metadata.MetadataService;
import com.kdgregory.example.javalambda.shared.services.metadata.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.services.metadata.Sizes;


/**
 *  Lambda handler invoked by image arriving in upload bucket. Verifies that we
 *  have metadata for that image, then moves it into images bucket and creates
 *  scaled images from it.
 */
public class Resizer
{
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private String uploadBucket;
    private MetadataService metadataService;
    private ContentService contentService;

    public Resizer()
    {
        uploadBucket = Environment.getOrThrow(Environment.S3_UPLOAD_BUCKET);
        
        metadataService = new MetadataService(
                            Environment.getOrThrow(Environment.DYNAMO_TABLE));
        contentService = new ContentService(
                            uploadBucket,
                            Environment.getOrThrow(Environment.S3_IMAGE_BUCKET));
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    public void handler(S3Event event, Context lambdaContext)
    {
        MDC.clear();
        MDC.put("requestId", lambdaContext.getAwsRequestId());

        logger.info("received {} record(s)", event.getRecords().size());

        for (S3EventNotificationRecord record : event.getRecords())
        {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getKey();
            
            if (! uploadBucket.equals(bucket))
            {
                logger.warn("ignoring invalid notification: s3://{}/{}", bucket, key);
                continue;
            }
            
            PhotoMetadata metadata = metadataService.retrieve(key);
            if (metadata == null)
            {
                logger.warn("ignoring notification with no metadata: {}", key);
                continue;
            }

            process(metadata);
        }
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    private void process(PhotoMetadata metadata)
    {
        String photoId = metadata.getId();
        logger.info("processing photo {} for user {}", photoId, metadata.getUser());
        try
        {
            BufferedImage img = loadImage(photoId);
            if (img == null)
            {
                logger.warn("no content for photo {}; skipping resize", photoId);
                return;
            }

            for (Sizes size : Sizes.values())
            {
                if (! metadata.getSizes().contains(size))
                {
                    resizeTo(metadata, img, size);
                }
            }
    
            metadataService.store(metadata);
        }
        catch (Exception ex)
        {
            logger.error("exception when processing photo {}", metadata.getId(), ex);
        }
        
    }


    /**
     *  Loads the content bytes into a buffered image and returns it, null if unable to
     *  load the image.
     */
    private BufferedImage loadImage(String photoId)
    {
        byte[] content = contentService.retrieve(photoId, Sizes.ORIGINAL);
        if (content == null)
            return null;

        try
        {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(content));
            logger.debug("original file size = {}, width = {}, height = {}",
                         content.length, img.getWidth(), img.getHeight());
            return img;
        }
        catch (Exception ex)
        {
            // this will be logged by caller
            return null;
        }
    }


    /**
     *  Attempts to resize the image, writing the resized image to S3. If successful, adds the
     *  new size to the passed metadata. The output image will have the same MIME type as the
     *  input image.
     */
    private void resizeTo(PhotoMetadata metadata, BufferedImage img, Sizes size)
    {
        try
        {
            double scaleFactor = 1.0 * size.getWidth() / img.getWidth();
            int dstWidth = size.getWidth();
            int dstHeight = (int)(img.getHeight() * scaleFactor);

            logger.debug("resizing to fit {}; actual dimensions are {} x {}", size.getDescription(), dstWidth, dstHeight);

            BufferedImage dst = new BufferedImage(dstWidth, dstHeight, img.getType());
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,        RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(img, 0, 0, dstWidth, dstHeight, null);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream(65536);
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(metadata.getMimetype());
            if (! writers.hasNext())
            {
                logger.error("no writer to support {}", metadata.getMimetype());
                return;
            }
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            writer.write(dst);

            contentService.store(metadata.getId(), metadata.getMimetype(), size, bos.toByteArray());
            metadata.getSizes().add(size);
        }
        catch (Exception ex)
        {
            logger.error("failed to write {}", size, ex);
        }
    }
}