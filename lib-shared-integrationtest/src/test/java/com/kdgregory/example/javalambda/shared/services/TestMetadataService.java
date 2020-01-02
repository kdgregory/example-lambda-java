// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.collections.CollectionUtil;

import com.kdgregory.example.javalambda.shared.data.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.data.PhotoMetadata.Fields;
import com.kdgregory.example.javalambda.shared.data.Sizes;
import com.kdgregory.example.javalambda.shared.services.impl.MetadataServiceImpl;


/**
 *  Exercises the metadata service using bogus IDs. This is assumed to run against
 *  an empty database during development testing, but should not cause an problem
 *  if run against a database with real photo data.
 */
public class TestMetadataService
{
    // this is the name that I used for development; update as appropriate
    private final static String DATABASE_NAME = "LambdaPhoto-Metadata";

    private Logger logger = LoggerFactory.getLogger(getClass());

    private MetadataService service;

    // some values that are used by many tests so created once
    private Long now;
    private String testPhotoId;
    private String testUsername;
    private String testFilename;
    private String testMimetype;
    private String testDescription;

//----------------------------------------------------------------------------
//  JUnit scaffolding
//----------------------------------------------------------------------------

    @Before
    public void setup()
    {
        service = new MetadataServiceImpl(DATABASE_NAME);

        now = System.currentTimeMillis();

        testPhotoId = UUID.randomUUID().toString();
        testUsername = "" + System.currentTimeMillis() + "@example.com";
        testFilename = "myphoto.jpg";
        testMimetype = "image/jpeg";
        testDescription = "something relevant";
    }

//----------------------------------------------------------------------------
//  Test cases
//----------------------------------------------------------------------------

    @Test
    public void testStoreAndRetrieve() throws Exception
    {
        logger.info("testCreateAndRetrieve");

        PhotoMetadata original = new PhotoMetadata(
                                    testPhotoId,
                                    testUsername,
                                    testFilename,
                                    testMimetype,
                                    testDescription,
                                    now,
                                    Arrays.asList(Sizes.ORIGINAL.name()));

        logger.debug("original metadata: {}", original);
        service.store(original);

        logger.debug("retrieving by ID");
        PhotoMetadata retrieved = service.retrieve(testPhotoId);

        assertEquals("retrieved metadata, id",              original.getId(),           retrieved.getId());
        assertEquals("retrieved metadata, user",            original.getUser(),         retrieved.getUser());
        assertEquals("retrieved metadata, filename",        original.getFilename(),     retrieved.getFilename());
        assertEquals("retrieved metadata, mimetype",        original.getMimetype(),     retrieved.getMimetype());
        assertEquals("retrieved metadata, description",     original.getDescription(),  retrieved.getDescription());
        assertEquals("retrieved metadata, uploadedAt",      original.getUploadedAt(),   retrieved.getUploadedAt());
        assertEquals("retrieved metadata, sizes",           original.getSizes(),        retrieved.getSizes());

        logger.debug("retrieving by username");
        List<PhotoMetadata> byUser = service.retrieveByUser(testUsername);

        assertEquals("photos returned for user",            1,                          byUser.size());
        assertEquals("retrieved metadata, id",              original.getId(),           byUser.get(0).getId());
        assertEquals("retrieved metadata, user",            original.getUser(),         byUser.get(0).getUser());
        assertEquals("retrieved metadata, filename",        original.getFilename(),     byUser.get(0).getFilename());
        assertEquals("retrieved metadata, mimetype",        original.getMimetype(),     byUser.get(0).getMimetype());
        assertEquals("retrieved metadata, description",     original.getDescription(),  byUser.get(0).getDescription());
        assertEquals("retrieved metadata, uploadedAt",      original.getUploadedAt(),   byUser.get(0).getUploadedAt());
        assertEquals("retrieved metadata, sizes",           original.getSizes(),        byUser.get(0).getSizes());

        logger.debug("deleting metadata");
        service.delete(testPhotoId);

        PhotoMetadata postDelete = service.retrieve(testPhotoId);
        assertNull("retrieve returned null after delete", postDelete);
    }


    @Test
    public void testStoreInitialMetadata() throws Exception
    {
        logger.info("testStoreInitialMetadata");

        Map<String,Object> clientMap = new HashMap<>();
        clientMap.put(Fields.USERNAME, testUsername);
        clientMap.put(Fields.FILENAME, testFilename);
        clientMap.put(Fields.DESCRIPTION, testDescription);
        clientMap.put(Fields.MIMETYPE, testMimetype);

        PhotoMetadata original = PhotoMetadata.fromClientMap(clientMap);
        logger.debug("original metadata: {}", original);

        service.store(original);
        PhotoMetadata retrieved = service.retrieve(original.getId());

        assertEquals("retrieved metadata, id",              original.getId(),           retrieved.getId());
        assertEquals("retrieved metadata, user",            original.getUser(),         retrieved.getUser());
        assertEquals("retrieved metadata, filename",        original.getFilename(),     retrieved.getFilename());
        assertEquals("retrieved metadata, mimetype",        original.getMimetype(),     retrieved.getMimetype());
        assertEquals("retrieved metadata, description",     original.getDescription(),  retrieved.getDescription());
        assertEquals("retrieved metadata, uploadedAt",      original.getUploadedAt(),   retrieved.getUploadedAt());
        assertEquals("retrieved metadata, sizes",           original.getSizes(),        retrieved.getSizes());

        logger.debug("deleting metadata");
        service.delete(original.getId());
    }


    @Test
    public void testStoreWithoutDescription() throws Exception
    {
        logger.info("testStoreWithoutDescription");

        Map<String,Object> clientMap = new HashMap<>();
        clientMap.put(Fields.USERNAME, testUsername);
        clientMap.put(Fields.FILENAME, testFilename);
        clientMap.put(Fields.MIMETYPE, testMimetype);

        PhotoMetadata original = PhotoMetadata.fromClientMap(clientMap);
        logger.debug("original metadata: {}", original);

        service.store(original);
        PhotoMetadata retrieved = service.retrieve(original.getId());

        assertEquals("retrieved metadata, id",              original.getId(),           retrieved.getId());
        assertEquals("retrieved metadata, user",            original.getUser(),         retrieved.getUser());
        assertEquals("retrieved metadata, filename",        original.getFilename(),     retrieved.getFilename());
        assertEquals("retrieved metadata, mimetype",        original.getMimetype(),     retrieved.getMimetype());
        assertEquals("retrieved metadata, description",     original.getDescription(),  retrieved.getDescription());
        assertEquals("retrieved metadata, uploadedAt",      original.getUploadedAt(),   retrieved.getUploadedAt());
        assertEquals("retrieved metadata, sizes",           original.getSizes(),        retrieved.getSizes());

        logger.debug("deleting metadata");
        service.delete(original.getId());
    }


    @Test
    public void testRetrieveByUser() throws Exception
    {
        logger.info("testRetrieveByUser");

        // it's easier to modify the client map, so we'll go with it
        Map<String,Object> clientMap = new HashMap<>();
        clientMap.put(Fields.USERNAME, testUsername);
        clientMap.put(Fields.FILENAME, testFilename);
        clientMap.put(Fields.DESCRIPTION, testDescription);
        clientMap.put(Fields.MIMETYPE, testMimetype);

        PhotoMetadata meta1 = PhotoMetadata.fromClientMap(clientMap);
        logger.debug("original metadata #1: {}", meta1);
        service.store(meta1);

        clientMap.put(Fields.FILENAME, "anotherfile.jpg");
        PhotoMetadata meta2 = PhotoMetadata.fromClientMap(clientMap);
        logger.debug("original metadata #2: {}", meta2);
        service.store(meta2);

        logger.debug("retrieving by username");
        List<PhotoMetadata> byUser = service.retrieveByUser(testUsername);

        assertEquals("retrieved photos", 2, byUser.size());

        Set<String> expectedIds = CollectionUtil.asSet(meta1.getId(), meta2.getId());
        Set<String> actualIds = byUser.stream().map(PhotoMetadata::getId).collect(Collectors.toSet());
        assertEquals("retrieval contained all expected IDs", expectedIds, actualIds);

        Set<String> expectedNames = CollectionUtil.asSet(meta1.getFilename(), meta2.getFilename());
        Set<String> actualNames = byUser.stream().map(PhotoMetadata::getFilename).collect(Collectors.toSet());
        assertEquals("retrieval contained all expected filenames", expectedNames, actualNames);

        logger.debug("deleting metadata");
        service.delete(meta1.getId());
        service.delete(meta2.getId());
    }
}
