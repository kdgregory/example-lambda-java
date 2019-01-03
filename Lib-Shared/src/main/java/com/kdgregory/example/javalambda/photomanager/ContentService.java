// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.io.IOUtil;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import com.kdgregory.example.javalambda.photomanager.tabledef.Sizes;


/**
 *  This class supports management of a photo's content.
 */
public class ContentService
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonS3 s3Client;
    private String s3BucketName;
    private String s3ImagePrefix;

    public ContentService(String s3BucketName, String s3ImagePrefix)
    {
        s3Client = AmazonS3ClientBuilder.defaultClient();
        this.s3BucketName = s3BucketName;
        this.s3ImagePrefix = s3ImagePrefix;
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Retrieves the content for a photo, null if unable to find the photo.
     */
    public byte[] download(String photoId, Sizes size)
    {
        S3Object s3Obj = null;
        try
        {
            s3Obj = s3Client.getObject(s3BucketName, s3Key(photoId, size));
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            IOUtil.copy(s3Obj.getObjectContent(), buf);
            return buf.toByteArray();
        }
        catch (Exception ex)
        {
            logger.error("unable to read content for {} size {}", photoId, size.name(), ex);
            return null;
        }
        finally
        {
            if (s3Obj != null)  IOUtil.closeQuietly(s3Obj.getObjectContent());
        }
    }
    
    
    /**
     *  Stores the content for a photo.
     */
    public void upload(String photoId, String mimeType, Sizes size, byte[] content)
    {
        logger.debug("uploading photo: id = {}, size = {}, content-length = {}",
                     photoId, size.name(), content.length);

        ObjectMetadata s3Meta = new ObjectMetadata();
        s3Meta.setContentLength(content.length);
        s3Meta.setContentType(mimeType);
        PutObjectResult s3Response = s3Client.putObject(
                                        s3BucketName,
                                        s3Key(photoId, size),
                                        new ByteArrayInputStream(content),
                                        s3Meta);

        logger.debug("upload successful, etag = " + s3Response.getETag());
    }

    
//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------
    
    /**
     *  Returns the storage path for an object.
     */
    private String s3Key(String photoId, Sizes size)
    {
        return s3ImagePrefix + "/" + photoId + "/" + size.name();
    }
}
