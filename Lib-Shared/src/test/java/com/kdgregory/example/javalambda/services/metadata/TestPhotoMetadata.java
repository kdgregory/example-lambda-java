// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.services.metadata;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.kdgregory.example.javalambda.services.metadata.Fields;
import com.kdgregory.example.javalambda.services.metadata.PhotoMetadata;
import com.kdgregory.example.javalambda.services.metadata.Sizes;

import org.junit.Test;
import static org.junit.Assert.*;

import net.sf.kdgcommons.collections.CollectionUtil;


public class TestPhotoMetadata
{
    @Test
    public void testClientMap() throws Exception
    {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Fields.ID,            "abcd");
        map.put(Fields.USERNAME,      "qwerty");
        map.put(Fields.FILENAME,      "test");
        map.put(Fields.MIMETYPE,      "image/jpeg");
        map.put(Fields.DESCRIPTION,   "this is a test");
        map.put(Fields.UPLOADED_AT,   123L);

        PhotoMetadata meta = PhotoMetadata.fromClientMap(map);

        assertEquals(Fields.ID,           "abcd",                       meta.getId());
        assertEquals(Fields.USERNAME,     "qwerty",                     meta.getUser());
        assertEquals(Fields.FILENAME,     "test",                       meta.getFilename());
        assertEquals(Fields.MIMETYPE,     "image/jpeg",                 meta.getMimetype());
        assertEquals(Fields.DESCRIPTION,  "this is a test",             meta.getDescription());
        assertEquals(Fields.UPLOADED_AT,  Long.valueOf(123),            meta.getUploadedAt());
        assertEquals(Fields.SIZES,        EnumSet.of(Sizes.ORIGINAL),   meta.getSizes());

        assertTrue("valid", meta.isValid());

        Map<String,Object> created = meta.toClientMap();
        List<Map<String,Object>> sizes = (List<Map<String,Object>>)created.remove(Fields.SIZES);
        assertEquals("created map contains default size", 1, sizes.size());
        assertEquals("created map contains default size", Sizes.ORIGINAL.name(), sizes.get(0).get("name"));
        assertEquals("created map, remaining fields are as provided",  map, created);
    }


    @Test
    public void testDynamoMap() throws Exception
    {
        Map<String,AttributeValue> map = new HashMap<String,AttributeValue>();
        map.put(Fields.ID,            new AttributeValue().withS("abcd"));
        map.put(Fields.USERNAME,      new AttributeValue().withS("qwerty"));
        map.put(Fields.FILENAME,      new AttributeValue().withS("test"));
        map.put(Fields.MIMETYPE,      new AttributeValue().withS("image/jpeg"));
        map.put(Fields.DESCRIPTION,   new AttributeValue().withS("this is a test"));
        map.put(Fields.UPLOADED_AT,   new AttributeValue().withN("123"));
        map.put(Fields.SIZES,         new AttributeValue().withSS(Sizes.ORIGINAL.name()));

        PhotoMetadata meta = PhotoMetadata.fromDynamoMap(map);

        assertEquals(Fields.ID,           "abcd",                       meta.getId());
        assertEquals(Fields.USERNAME,     "qwerty",                     meta.getUser());
        assertEquals(Fields.FILENAME,     "test",                       meta.getFilename());
        assertEquals(Fields.MIMETYPE,     "image/jpeg",                 meta.getMimetype());
        assertEquals(Fields.DESCRIPTION,  "this is a test",             meta.getDescription());
        assertEquals(Fields.UPLOADED_AT,  Long.valueOf(123),            meta.getUploadedAt());
        assertEquals(Fields.SIZES,        EnumSet.of(Sizes.ORIGINAL),   meta.getSizes());

        assertTrue("valid", meta.isValid());

        assertEquals("created map equals original", map, meta.toDynamoMap());
    }


    @Test
    public void testDefaultValues() throws Exception
    {
        PhotoMetadata meta = PhotoMetadata.fromClientMap(Collections.emptyMap());

        assertFalse("valid metadata", meta.isValid());

        // only these things have defaults

        assertEquals(Fields.DESCRIPTION,  "",                                         meta.getDescription());
        assertEquals(Fields.SIZES,        EnumSet.of(Sizes.ORIGINAL),   meta.getSizes());

        // Dynamo fields can't be empty so we'll omit them

        assertEquals("Dynamo map only holds non-blank entries",
                     CollectionUtil.asSet(Fields.SIZES),
                     meta.toDynamoMap().keySet());
    }
}
