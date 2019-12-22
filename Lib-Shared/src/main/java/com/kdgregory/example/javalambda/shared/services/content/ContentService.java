// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services.content;

import java.io.ByteArrayInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.io.IOUtil;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import com.kdgregory.example.javalambda.shared.services.metadata.Sizes;


/**
 *  This class supports management of a photo's content.
 */
public class ContentService
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonS3 s3Client;
    private String uploadBucket;
    private String imageBucket;


    public ContentService(AmazonS3 s3Client, String uploadBucket, String imageBucket)
    {
        this.s3Client = s3Client;
        this.uploadBucket = uploadBucket;
        this.imageBucket = imageBucket;
    }


    public ContentService(String uploadBucket, String imageBucket)
    {
        this(AmazonS3ClientBuilder.defaultClient(), uploadBucket, imageBucket);
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Stores the content for a photo at a given size.
     */
    public void store(String photoId, String mimeType, Sizes size, byte[] content)
    {
        logger.debug("uploading: photo {}, size = {}, content-length = {}",
                     photoId, size.name(), content.length);

        ObjectMetadata s3Meta = new ObjectMetadata();
        s3Meta.setContentLength(content.length);
        s3Meta.setContentType(mimeType);
        PutObjectResult s3Response = s3Client.putObject(
                                        imageBucket,
                                        s3Key(photoId, size),
                                        new ByteArrayInputStream(content),
                                        s3Meta);

        logger.debug("upload successful: photo {}, etag {}", photoId, s3Response.getETag());
    }


    /**
     *  Retrieves the content for a photo at a given size, null if unable to find the photo.
     */
    public byte[] retrieve(String photoId, Sizes size)
    {
        logger.debug("retrieving content for photo {}, size {}", photoId, size);
        S3Object s3Obj = null;
        try
        {
            s3Obj = s3Client.getObject(imageBucket, s3Key(photoId, size));
            long contentLength = s3Obj.getObjectMetadata().getContentLength();
            if (contentLength > Integer.MAX_VALUE)
            {
                logger.error("photo {} is too large to read into a byte array: {} bytes", photoId, contentLength);
                return null;
            }

            byte[] content = new byte[(int)contentLength];
            IOUtil.readFully(s3Obj.getObjectContent(), content);
            logger.debug("retrieved {} bytes for photo {}", content.length, photoId);
            return content;
        }
        catch (AmazonS3Exception ex)
        {
            if (ex.getStatusCode() == 404)
            {
                logger.warn("photo {} size {} does not exist", photoId, size.name());
                return null;
            }

            logger.error("unexpected exception retrieving photo {} size {}", photoId, size.name(), ex);
            return null;
        }
        catch (Exception ex)
        {
            logger.error("unexpected exception retrieving photo {} size {}", photoId, size.name(), ex);
            return null;
        }
        finally
        {
            if (s3Obj != null)  IOUtil.closeQuietly(s3Obj.getObjectContent());
        }
    }


    /**
     *  Moves an uploaded photo from the upload bucket to the image bucket, storing
     *  it as "ORIGINAL" size.
     */
    public void moveUploadToImageBucket(String photoId)
    {
        String destname = photoId + "/" + Sizes.ORIGINAL.name();
        logger.debug("moving object s3://{}/{} to s3://{}/{}",
                     uploadBucket, photoId, imageBucket, destname);
        s3Client.copyObject(uploadBucket, photoId, imageBucket, destname);
        s3Client.deleteObject(uploadBucket, photoId);
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    /**
     *  Returns the storage path for an object.
     */
    private String s3Key(String photoId, Sizes size)
    {
        return photoId + "/" + size.name();
    }
}
