// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.codec.Base64Codec;
import net.sf.kdgcommons.lang.StringUtil;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.kdgregory.example.javalambda.shared.config.Environment;
import com.kdgregory.example.javalambda.shared.messages.NewPhoto;
import com.kdgregory.example.javalambda.shared.services.content.ContentService;
import com.kdgregory.example.javalambda.shared.services.metadata.Fields;
import com.kdgregory.example.javalambda.shared.services.metadata.MetadataService;
import com.kdgregory.example.javalambda.shared.services.metadata.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.services.metadata.Sizes;
import com.kdgregory.example.javalambda.webapp.util.Request;
import com.kdgregory.example.javalambda.webapp.util.Response;
import com.kdgregory.example.javalambda.webapp.util.ResponseCodes;


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

    private AmazonSNS snsClient;
    private ObjectMapper objectMapper;
    private String snsTopicArn;
    private MetadataService metadataService;
    private ContentService contentService;


    public PhotoService()
    {
        snsClient = AmazonSNSClientBuilder.defaultClient();
        objectMapper = new ObjectMapper();
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
        logger.info("listPhotos: user {}", userId);

         List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
         for (PhotoMetadata item : metadataService.retrieve(userId, null))
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
        Map<String,Object> requestBody = request.getBody();
        String userId = request.getUser();
        String filename = (String)requestBody.get(ParamNames.FILENAME);
        String photoId = UUID.randomUUID().toString();

        logger.info("upload: user {}, filename {}, photo {}", userId, filename, photoId);

        PhotoMetadata metadata = new PhotoMetadata(
                                    photoId,
                                    userId,
                                    filename,
                                    (String)requestBody.get(Fields.MIMETYPE),
                                    (String)requestBody.get(Fields.DESCRIPTION),
                                    Long.valueOf(System.currentTimeMillis()),
                                    Arrays.asList(Sizes.ORIGINAL.name()));
        if (! metadata.isValid())
        {
            logger.warn("missing metadata; provided keys: {}", requestBody.keySet());
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        String base64Content = (String)request.getBody().get(ParamNames.CONTENT);
        if (base64Content == null)
        {
            logger.warn("missing content");
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

        try
        {
            String uploadMessage = objectMapper.writeValueAsString(new NewPhoto(userId, photoId));
            snsClient.publish(snsTopicArn, uploadMessage);
        }
        catch (Exception ex)
        {
            logger.error("unable to write message to SNS: user {}, filename {}, photo {}", userId, filename, photoId, ex);
            // FIXME - this should just throw
            return new Response(ResponseCodes.INTERNAL_ERROR);
        }

        return new Response(ResponseCodes.SUCCESS);
    }
}
