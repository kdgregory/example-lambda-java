// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.kdgcommons.io.IOUtil;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import com.kdgregory.example.javalambda.shared.services.content.ContentService;
import com.kdgregory.example.javalambda.shared.services.metadata.Sizes;


public class TestContentService
{
    private final static String TEST_IMAGE_FILENAME = "/test-image.jpg";
    private final static String TEST_IMAGE_MIMETYPE = "image/jpeg";

    private final static String UPLOAD_BUCKET_NAME = "com-kdgregory-lambdaphoto-uploads";
    private final static String IMAGE_BUCKET_NAME = "com-kdgregory-lambdaphoto-images";

    private static AmazonS3 s3Client;

    private Logger logger = LoggerFactory.getLogger(getClass());

    // these are created per-test
    private ContentService service;
    private byte[] content;
    private String photoId;
    private String objectKey;

//----------------------------------------------------------------------------
//  Helpers
//----------------------------------------------------------------------------

    /**
     *  This function should move into KDGCommons
     */
    private static byte[] load(InputStream in) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024*1024);
        IOUtil.copy(in, buffer);
        return buffer.toByteArray();
    }

//----------------------------------------------------------------------------
//  Scaffolding
//----------------------------------------------------------------------------

    @BeforeClass
    public static void init() throws Exception
    {
        s3Client = AmazonS3ClientBuilder.defaultClient();
    }


    @AfterClass
    public static void finish() throws Exception
    {
        s3Client.shutdown();
    }


    @Before
    public void setUp() throws Exception
    {
        service = new ContentService(s3Client, UPLOAD_BUCKET_NAME, IMAGE_BUCKET_NAME);

        try (InputStream contentSource = getClass().getResourceAsStream(TEST_IMAGE_FILENAME))
        {
            content = load(contentSource);
        }

        photoId = UUID.randomUUID().toString();
        objectKey =  photoId + "/" + Sizes.ORIGINAL.name();
    }


    @After
    public void tearDown() throws Exception
    {
        // nothing here (yet)
    }

//----------------------------------------------------------------------------
//  Testcases
//----------------------------------------------------------------------------

    @Test
    public void testStoreAndRetrieve() throws Exception
    {
        logger.info("testStoreAndRetrieve: store({})", photoId);
        service.store(photoId, TEST_IMAGE_MIMETYPE, Sizes.ORIGINAL, content);

        logger.info("testStoreAndRetrieve: comparing to S3");
        S3Object object = s3Client.getObject(IMAGE_BUCKET_NAME, objectKey);
        assertEquals("object size as reported by S3",
                     (long)content.length,
                     object.getObjectMetadata().getContentLength());
        assertEquals("mimetype as reported by S3",
                     TEST_IMAGE_MIMETYPE,
                     object.getObjectMetadata().getContentType());

        try (InputStream s3is = object.getObjectContent())
        {
            assertArrayEquals("object content from s3", content, load(s3is));
        }

        logger.info("testStoreAndRetrieve: retrieve({})", photoId);
        byte[] retrieved = service.retrieve(photoId, Sizes.ORIGINAL);
        assertArrayEquals("object content from retrieve()", content, retrieved);

        s3Client.deleteObject(IMAGE_BUCKET_NAME, objectKey);
    }


    @Test
    public void testRetrieveNonexistentPhoto() throws Exception
    {
        logger.info("testRetrieveNonexistentPhoto");
        assertNull(service.retrieve(photoId, Sizes.ORIGINAL));
    }


    @Test
    public void testMove() throws Exception
    {
        logger.info("testMove: uploading file with key {}", photoId);
        ObjectMetadata srcMeta = new ObjectMetadata();
        srcMeta.setContentLength(content.length);
        srcMeta.setContentType(TEST_IMAGE_MIMETYPE);
        s3Client.putObject(UPLOAD_BUCKET_NAME, photoId, new ByteArrayInputStream(content), srcMeta);

        logger.info("testMove: invoking move");
        service.moveUploadToImageBucket(photoId);

        ObjectMetadata dstMeta = s3Client.getObjectMetadata(IMAGE_BUCKET_NAME, objectKey);
        assertEquals("moved file content length", srcMeta.getContentLength(), dstMeta.getContentLength());
        assertEquals("moved file mime type", srcMeta.getContentType(), dstMeta.getContentType());

        logger.info("testMove: retrieving result");
        byte[] retrieved = service.retrieve(photoId, Sizes.ORIGINAL);
        assertArrayEquals("moved file content", content, retrieved);

        try
        {
            s3Client.getObjectMetadata(UPLOAD_BUCKET_NAME, photoId);
            fail("able to retrieve upload metadata after move");
        }
        catch (AmazonS3Exception ex)
        {
            if (ex.getStatusCode() != 404)
                throw ex;
            // otherwise success
        }
    }
}
