// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdgregory.example.javalambda.shared.config.Environment;
import com.kdgregory.example.javalambda.shared.data.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.data.PhotoMetadata.Fields;
import com.kdgregory.example.javalambda.shared.services.ContentService;
import com.kdgregory.example.javalambda.shared.services.MetadataService;
import com.kdgregory.example.javalambda.shared.services.impl.ContentServiceImpl;
import com.kdgregory.example.javalambda.shared.services.impl.MetadataServiceImpl;

import com.kdgregory.example.javalambda.webapp.util.Request;
import com.kdgregory.example.javalambda.webapp.util.Response;
import com.kdgregory.example.javalambda.webapp.util.ResponseCodes;


/**
 *  Manages the photo database, including upload, list, and download.
 */
public class PhotoService
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private MetadataService metadataService;
    private ContentService contentService;


    public PhotoService()
    {
        metadataService = new MetadataServiceImpl(
                            Environment.getOrThrow(Environment.DYNAMO_TABLE));
        contentService = new ContentServiceImpl(
                            Environment.getOrThrow(Environment.S3_UPLOAD_BUCKET),
                            Environment.getOrThrow(Environment.S3_IMAGE_BUCKET));
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
        logger.info("listPhotos: {}", userId);

         List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
         for (PhotoMetadata item : metadataService.retrieveByUser(userId))
         {
             result.add(item.toClientMap());
         }

        logger.debug("listPhotos: {} photos for user {}", result.size(), userId);
        return new Response(ResponseCodes.SUCCESS, result);
    }


    /**
     *  Creates metadata for a new photo upload. Returns a signed URL that the client
     *  can use for the actual upload.
     */
    public Response prepareUpload(Request request)
    {
        Map<String,Object> requestBody = request.getBody();
        requestBody.put(Fields.USERNAME, request.getUser());    // FIXME - this is a hack
        PhotoMetadata metadata = PhotoMetadata.fromClientMap(requestBody);

        if (! metadata.isValid())
        {
            logger.warn("upload: missing metadata; provided keys: {}", requestBody.keySet());
            return new Response(ResponseCodes.INVALID_OPERATION);
        }

        logger.info("upload: {}", metadata);

        metadataService.store(metadata);

        return new Response(ResponseCodes.SUCCESS,
                            contentService.createUploadURL(metadata.getId()));
    }
}
