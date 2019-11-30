// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdgregory.example.javalambda.shared.services.metadata.MetadataService;
import com.kdgregory.example.javalambda.shared.services.metadata.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.services.metadata.Sizes;


/**
 *  Exercises the metadata service using bogus IDs. This is assumed to run against
 *  an empty database during development testing, but should not cause an problem
 *  if run against a database with real photo data.
 */
public class TestMetadataService
{
    private final static String DATABASE_NAME = "LambdaPhoto-Metadata";

    private Logger logger = LoggerFactory.getLogger(getClass());


    @Test
    public void testBasicOperation() throws Exception
    {
        MetadataService service = new MetadataService(DATABASE_NAME);

        final String photoId = UUID.randomUUID().toString();
        final String username = "" + System.currentTimeMillis() + "@example.com";
        final Long uploadedAt = System.currentTimeMillis();

        PhotoMetadata original = new PhotoMetadata(
                                    photoId,
                                    username,
                                    "myphoto.jpg",                          // filename
                                    "image/jpeg",                           // mimetype
                                    "something relevant",                   // description
                                    uploadedAt,
                                    Arrays.asList(Sizes.ORIGINAL.name()));  // sizes

        logger.info("testCreateAndRetrieve: photoId = {}, userId = {}, uploadTime = {}",
                    photoId, username, uploadedAt);

        logger.debug("storing original");
        service.store(original);

        logger.debug("retrieving by ID");
        PhotoMetadata retrieved = service.retrieve(photoId);

        assertEquals("retrieved metadata, id",              original.getId(),           retrieved.getId());
        assertEquals("retrieved metadata, user",            original.getUser(),         retrieved.getUser());
        assertEquals("retrieved metadata, filename",        original.getFilename(),     retrieved.getFilename());
        assertEquals("retrieved metadata, mimetype",        original.getMimetype(),     retrieved.getMimetype());
        assertEquals("retrieved metadata, description",     original.getDescription(),  retrieved.getDescription());
        assertEquals("retrieved metadata, uploadedAt",      original.getUploadedAt(),   retrieved.getUploadedAt());
        assertEquals("retrieved metadata, sizes",           original.getSizes(),        retrieved.getSizes());

        logger.debug("retrieving by username");
        List<PhotoMetadata> byUser = service.retrieveByUser(username);

        assertEquals("photos returned for user",            1,                          byUser.size());
        assertEquals("retrieved metadata, id",              original.getId(),           byUser.get(0).getId());
        assertEquals("retrieved metadata, user",            original.getUser(),         byUser.get(0).getUser());
        assertEquals("retrieved metadata, filename",        original.getFilename(),     byUser.get(0).getFilename());
        assertEquals("retrieved metadata, mimetype",        original.getMimetype(),     byUser.get(0).getMimetype());
        assertEquals("retrieved metadata, description",     original.getDescription(),  byUser.get(0).getDescription());
        assertEquals("retrieved metadata, uploadedAt",      original.getUploadedAt(),   byUser.get(0).getUploadedAt());
        assertEquals("retrieved metadata, sizes",           original.getSizes(),        byUser.get(0).getSizes());

        logger.debug("deleting metadata");
        service.delete(photoId);

        PhotoMetadata postDelete = service.retrieve(photoId);
        assertNull("retrieve returned null after delete", postDelete);
    }

}
