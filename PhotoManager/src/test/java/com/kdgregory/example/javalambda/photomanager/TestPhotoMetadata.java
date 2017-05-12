// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import org.junit.Test;
import static org.junit.Assert.*;

import net.sf.kdgcommons.collections.CollectionUtil;


public class TestPhotoMetadata
{
    @Test
    public void testClientMap() throws Exception
    {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(PhotoMetadata.Fields.ID,            "abcd");
        map.put(PhotoMetadata.Fields.USER,          "qwerty");
        map.put(PhotoMetadata.Fields.FILENAME,      "test");
        map.put(PhotoMetadata.Fields.MIMETYPE,      "image/jpeg");
        map.put(PhotoMetadata.Fields.DESCRIPTION,   "this is a test");
        map.put(PhotoMetadata.Fields.UPLOADED_AT,   123L);

        PhotoMetadata meta = PhotoMetadata.fromClientMap(map);

        assertEquals(PhotoMetadata.Fields.ID,           "abcd",                                     meta.getId());
        assertEquals(PhotoMetadata.Fields.USER,         "qwerty",                                   meta.getUser());
        assertEquals(PhotoMetadata.Fields.FILENAME,     "test",                                     meta.getFilename());
        assertEquals(PhotoMetadata.Fields.MIMETYPE,     "image/jpeg",                               meta.getMimetype());
        assertEquals(PhotoMetadata.Fields.DESCRIPTION,  "this is a test",                           meta.getDescription());
        assertEquals(PhotoMetadata.Fields.UPLOADED_AT,  Long.valueOf(123),                          meta.getUploadedAt());
        assertEquals(PhotoMetadata.Fields.SIZES,        EnumSet.of(PhotoMetadata.Sizes.FULLSIZE),   meta.getSizes());

        assertTrue("valid", meta.isValid());

        Map<String,Object> created = meta.toClientMap();
        assertEquals("created map, default sizes",      Arrays.asList("FULLSIZE"),  created.remove(PhotoMetadata.Fields.SIZES));
        assertEquals("created map, remaining fields",   map,                        created);
    }


    @Test
    public void testDynamoMap() throws Exception
    {
        Map<String,AttributeValue> map = new HashMap<String,AttributeValue>();
        map.put(PhotoMetadata.Fields.ID,            new AttributeValue().withS("abcd"));
        map.put(PhotoMetadata.Fields.USER,          new AttributeValue().withS("qwerty"));
        map.put(PhotoMetadata.Fields.FILENAME,      new AttributeValue().withS("test"));
        map.put(PhotoMetadata.Fields.MIMETYPE,      new AttributeValue().withS("image/jpeg"));
        map.put(PhotoMetadata.Fields.DESCRIPTION,   new AttributeValue().withS("this is a test"));
        map.put(PhotoMetadata.Fields.UPLOADED_AT,   new AttributeValue().withN("123"));
        map.put(PhotoMetadata.Fields.SIZES,         new AttributeValue().withSS("FULLSIZE"));

        PhotoMetadata meta = PhotoMetadata.fromDynamoMap(map);

        assertEquals(PhotoMetadata.Fields.ID,           "abcd",                                     meta.getId());
        assertEquals(PhotoMetadata.Fields.USER,         "qwerty",                                   meta.getUser());
        assertEquals(PhotoMetadata.Fields.FILENAME,     "test",                                     meta.getFilename());
        assertEquals(PhotoMetadata.Fields.MIMETYPE,     "image/jpeg",                               meta.getMimetype());
        assertEquals(PhotoMetadata.Fields.DESCRIPTION,  "this is a test",                           meta.getDescription());
        assertEquals(PhotoMetadata.Fields.UPLOADED_AT,  Long.valueOf(123),                          meta.getUploadedAt());
        assertEquals(PhotoMetadata.Fields.SIZES,        EnumSet.of(PhotoMetadata.Sizes.FULLSIZE),   meta.getSizes());

        assertTrue("valid", meta.isValid());

        assertEquals("created map equals original", map, meta.toDynamoMap());
    }


    @Test
    public void testDefaultValues() throws Exception
    {
        PhotoMetadata meta = PhotoMetadata.fromClientMap(Collections.emptyMap());

        assertFalse("valid metadata", meta.isValid());

        // only these things have defaults

        assertEquals(PhotoMetadata.Fields.DESCRIPTION,  "",                                         meta.getDescription());
        assertEquals(PhotoMetadata.Fields.SIZES,        EnumSet.of(PhotoMetadata.Sizes.FULLSIZE),   meta.getSizes());

        // Dynamo fields can't be empty so we'll omit them

        assertEquals("Dynamo map only holds non-blank entries",
                     CollectionUtil.asSet(PhotoMetadata.Fields.SIZES),
                     meta.toDynamoMap().keySet());
    }
}
