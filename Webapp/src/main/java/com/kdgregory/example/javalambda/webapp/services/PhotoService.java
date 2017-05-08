// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.codec.Base64Codec;
import net.sf.kdgcommons.collections.MapBuilder;
import net.sf.kdgcommons.lang.ObjectUtil;
import net.sf.kdgcommons.lang.StringUtil;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

import com.kdgregory.example.javalambda.webapp.Request;
import com.kdgregory.example.javalambda.webapp.Response;
import com.kdgregory.example.javalambda.webapp.ResponseCodes;
import com.kdgregory.example.javalambda.webapp.util.Environment;


/**
 *  Manages the photo database, including upload, list, and download.
 */
public class PhotoService
{
    /**
     *  This is the data model for both uploads and downloads; see comments
     *  for which values are upload-only.
     */
    public static class ParamNames
    {
        public final static String  FILENAME    = "fileName";
        public final static String  FILESIZE    = "fileSize";
        public final static String  FILETYPE    = "fileType";
        public final static String  DESCRIPTION = "description";
        public final static String  CONTENT     = "content";
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonS3Client s3Client = new AmazonS3Client();

    private String s3BucketName;
    private String s3ImagePrefix;


    public PhotoService()
    {
        s3BucketName = Environment.getOrThrow(Environment.S3_IMAGE_BUCKET);
        s3ImagePrefix = Environment.getOrThrow(Environment.S3_IMAGE_PREFIX);
    }


    /**
     *  Holder for an upload request, which verifies that all fields are present
     *  and transforms any that need it.
     */
    private static class UploadRequest
    {
        String filename;
        String contentType;
        long fileSize;
        String description;
        byte[] content;

        public Map<String,Object> invalidFields = new TreeMap<String,Object>();

        public UploadRequest(Request request)
        {
            filename = (String)request.getBody().get(ParamNames.FILENAME);
            if (StringUtil.isBlank(filename))
                invalidFields.put(ParamNames.FILENAME, filename);

            contentType = (String)request.getBody().get(ParamNames.FILETYPE);
            if (StringUtil.isBlank(contentType))
                invalidFields.put(ParamNames.FILETYPE, contentType);

            Number fileSizeObj = (Number)request.getBody().get(ParamNames.FILESIZE);
            if (fileSizeObj != null)
            {
                fileSize = fileSizeObj.longValue();
            }
            else
            {
                invalidFields.put(ParamNames.FILESIZE, null);
            }

            description = ObjectUtil.defaultValue((String)request.getBody().get(ParamNames.DESCRIPTION), "");

            String base64Content = (String)request.getBody().get(ParamNames.CONTENT);
            if (StringUtil.isEmpty(base64Content))
                invalidFields.put(ParamNames.CONTENT, null);
            else
            {
                try
                {
                    content = new Base64Codec().toBytes(base64Content);
                }
                catch (IllegalArgumentException ex)
                {
                    invalidFields.put(ParamNames.CONTENT, StringUtil.substr(base64Content, 0, 8) + "...");
                }
            }

        }
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Retrieves a list of the current user's photos.
     */
    public Response listPhotos(Request request)
    {
        List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
        
        // this only returns a single batch of objects -- it's here as a placeholder,
        // and will be replaced by a call to DynamoDB
        ObjectListing objects = s3Client.listObjects(s3BucketName, s3ImagePrefix);
        for (S3ObjectSummary obj : objects.getObjectSummaries())
        {
            result.add(new MapBuilder<String,Object>(new HashMap<String,Object>())
                       .put(ParamNames.FILENAME, obj.getKey().replaceFirst(s3ImagePrefix + "/", ""))
                       .put(ParamNames.FILESIZE, Long.valueOf(obj.getSize()))
                       .put(ParamNames.DESCRIPTION, "dummy description")
                       .toMap());
        }

        return new Response(ResponseCodes.SUCCESS, result);
    }


    /**
     *  Uploads a new photo.
     */
    public Response upload(Request request)
    {
        UploadRequest upload = new UploadRequest(request);
        if (upload.invalidFields.size() > 0)
        {
            logger.warn("invalid upload: " + upload.invalidFields);
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        logger.info("uploading: {}, content-type: {}, size: {}",
                    upload.filename, upload.contentType, upload.fileSize);


        ObjectMetadata s3Meta = new ObjectMetadata();
        s3Meta.setContentLength(upload.fileSize);
        s3Meta.setContentType(upload.contentType);
        PutObjectResult s3Response = s3Client.putObject(
                                        s3BucketName,
                                        s3ImagePrefix + "/" + upload.filename,
                                        new ByteArrayInputStream(upload.content),
                                        s3Meta);
        logger.debug("upload successful, etag = " + s3Response.getETag());
        return new Response(ResponseCodes.SUCCESS);
    }
}
