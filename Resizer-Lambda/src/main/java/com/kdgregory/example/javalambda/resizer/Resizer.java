// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.resizer;


import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.collections.CollectionUtil;

import com.amazonaws.services.lambda.runtime.Context;

import com.kdgregory.example.javalambda.config.Environment;
import com.kdgregory.example.javalambda.photomanager.ContentService;
import com.kdgregory.example.javalambda.photomanager.MetadataService;
import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoKey;
import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoMetadata;
import com.kdgregory.example.javalambda.photomanager.tabledef.Sizes;


/**
 *  Lambda handler: receives the request, extracts the information that we care
 *  about, calls a service method, and then packages the results.
 */
public class Resizer
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private MetadataService metadataService;
    private ContentService contentService;

    public Resizer()
    {
        metadataService = new MetadataService(
                            Environment.getOrThrow(Environment.DYNAMO_TABLE));
        contentService = new ContentService(
                            Environment.getOrThrow(Environment.S3_IMAGE_BUCKET),
                            Environment.getOrThrow(Environment.S3_IMAGE_PREFIX));
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    public void handler(Map<String,Object> message, Context lambdaContext)
    {
        List<Map<String,Object>> records = (List<Map<String,Object>>)message.get("Records");
        for (Map<String,Object> record : records)
        {
            String identifier = (String)CollectionUtil.getVia(record, "Sns", "Message");
            PhotoKey photoKey = new PhotoKey(identifier);
            if (photoKey.isValid())
            {
                logger.debug("processing: " + identifier);
                process(photoKey);
            }
            else
            {
                logger.warn("invalid key: " + identifier);
            }
        }
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    private void process(PhotoKey photoKey)
    {
        PhotoMetadata metadata = CollectionUtil.first(metadataService.retrieve(photoKey));
        if (metadata == null)
            return;

        byte[] content = contentService.download(photoKey.getPhotoId(), Sizes.ORIGINAL);
        if (content == null)
            return;

        BufferedImage img = loadImage(content);
        if (img == null)
            return;

        for (Sizes size : Sizes.values())
        {
            if (! metadata.getSizes().contains(size))
            {
                resizeTo(metadata, img, size);
            }
        }

        metadataService.store(metadata);
    }


    /**
     *  Loads the content bytes into a buffered image and returns it, null if unable to
     *  load the image.
     */
    private BufferedImage loadImage(byte[] content)
    {
        try
        {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(content));
            logger.debug("downloaded original file size = {}, width = {}, height = {}",
                         content.length, img.getWidth(), img.getHeight());
            return img;
        }
        catch (Exception ex)
        {
            logger.warn("unable to load image", ex);
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

            contentService.upload(metadata.getId(), metadata.getMimetype(), size, bos.toByteArray());
            metadata.getSizes().add(size);
        }
        catch (Exception ex)
        {
            logger.error("failed to write {}", size, ex);
        }
    }
}