// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.codec.Base64Codec;
import net.sf.kdgcommons.collections.MapBuilder;
import net.sf.kdgcommons.lang.StringUtil;

import com.amazonaws.services.sns.AmazonSNSClient;

import com.kdgregory.example.javalambda.config.Environment;
import com.kdgregory.example.javalambda.photomanager.ContentService;
import com.kdgregory.example.javalambda.photomanager.MetadataService;
import com.kdgregory.example.javalambda.photomanager.tabledef.Fields;
import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoKey;
import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoMetadata;
import com.kdgregory.example.javalambda.photomanager.tabledef.Sizes;
import com.kdgregory.example.javalambda.webapp.Request;
import com.kdgregory.example.javalambda.webapp.Response;
import com.kdgregory.example.javalambda.webapp.ResponseCodes;


/**
 *  Manages the photo database, including upload, list, and download.
 */
public class PhotoService
{
    /**
     *  The list of parameters for uploads. These match the field names of the
     *  data stored in Dynamo, with some additions and subtractions.
     */
    public static class ParamNames
    {
        public final static String  FILENAME    = Fields.FILENAME;
        public final static String  FILETYPE    = Fields.MIMETYPE;
        public final static String  DESCRIPTION = Fields.DESCRIPTION;
        public final static String  FILESIZE    = "filesize";
        public final static String  CONTENT     = "content";
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    private Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonSNSClient snsClient;
    private String snsTopicArn;
    private MetadataService metadataService;
    private ContentService contentService;


    public PhotoService()
    {
        snsClient = new AmazonSNSClient();
        snsTopicArn = Environment.getOrThrow(Environment.SNS_TOPIC_ARN);
        metadataService = new MetadataService(
                            Environment.getOrThrow(Environment.DYNAMO_TABLE));
        contentService = new ContentService(
                            Environment.getOrThrow(Environment.S3_IMAGE_BUCKET),
                            Environment.getOrThrow(Environment.S3_IMAGE_PREFIX));
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Retrieves a list of the current user's photos.
     */
    public Response listPhotos(Request request)
    {
        String userId = request.getUser();
        logger.debug("listPhotos for user {}", userId);

         List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
         for (PhotoMetadata item : metadataService.retrieve(new PhotoKey(userId, null)))
         {
             result.add(item.toClientMap());
         }

        return new Response(ResponseCodes.SUCCESS, result);
    }


    /**
     *  Uploads a new photo.
     */
    public Response upload(Request request)
    {
        String photoId = UUID.randomUUID().toString();
        String userId = request.getUser();

        // we'll jam our data into the request body ... immutability be damned!
        Map<String,Object> requestBody = new MapBuilder<String,Object>(request.getBody())
                                         .put(Fields.ID, photoId)
                                         .put(Fields.USERNAME, userId)
                                         .put(Fields.UPLOADED_AT, Long.valueOf(System.currentTimeMillis()))
                                         .toMap();

        logger.debug("upload of {} for user {}", requestBody.get(ParamNames.FILENAME), userId);

        PhotoMetadata metadata = PhotoMetadata.fromClientMap(requestBody);
        if (! metadata.isValid())
        {
            logger.debug("missing metadata; provided keys: {}", requestBody.keySet());
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        String base64Content = (String)request.getBody().get(ParamNames.CONTENT);
        if (base64Content == null)
        {
            logger.debug("missing content");
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        byte[] content = null;
        try
        {
            content = new Base64Codec().toBytes(base64Content);
        }
        catch (IllegalArgumentException ex)
        {
            logger.debug("invalid content: " + StringUtil.substr(base64Content, 0, 16) + "...");
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        Object contentSize = requestBody.get(ParamNames.FILESIZE);
        if ((contentSize == null) || !(contentSize instanceof Number) || (((Number)contentSize).intValue() != content.length))
        {
            logger.debug("invalid content size: {} (actual size was {})" + contentSize, content.length);
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        metadataService.store(metadata);
        contentService.upload(metadata.getId(), metadata.getMimetype(), Sizes.ORIGINAL, content);
        snsClient.publish(snsTopicArn, new PhotoKey(userId, photoId).toCombinedValue());

        return new Response(ResponseCodes.SUCCESS);
    }
}
