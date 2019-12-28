// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services.metadata;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.document.Item;

import static net.sf.kdgcommons.test.NumericAsserts.*;

import com.kdgregory.example.javalambda.shared.data.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.data.PhotoMetadata.Fields;
import com.kdgregory.example.javalambda.shared.data.Sizes;

import org.junit.Test;
import static org.junit.Assert.*;


public class TestPhotoMetadata
{
    private final static String     TEST_ID         = "abcd";
    private final static String     TEST_USER       = "qwerty";
    private final static String     TEST_FILE       = "example";
    private final static String     TEST_MIME       = "image/jpeg";
    private final static String     TEST_DESC       = "a description";
    private final static long       TEST_TIMESTAMP  = 1574947877761L;
    private final static Set<Sizes> TEST_SIZES      = EnumSet.of(Sizes.ORIGINAL, Sizes.W1024H768);
    private final static Set<String> TEST_SIZES_STR = TEST_SIZES.stream().map(Sizes::name).collect(Collectors.toSet());


    @Test
    public void testFromClientMap() throws Exception
    {
        long now = System.currentTimeMillis();

        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Fields.ID,              TEST_ID);
        map.put(Fields.USERNAME,        TEST_USER);
        map.put(Fields.FILENAME,        TEST_FILE);
        map.put(Fields.MIMETYPE,        TEST_MIME);
        map.put(Fields.DESCRIPTION,     TEST_DESC);

        PhotoMetadata meta = PhotoMetadata.fromClientMap(map);

        assertEquals(Fields.ID,           TEST_ID,                              meta.getId());
        assertEquals(Fields.USERNAME,     TEST_USER,                            meta.getUser());
        assertEquals(Fields.FILENAME,     TEST_FILE,                            meta.getFilename());
        assertEquals(Fields.MIMETYPE,     TEST_MIME,                            meta.getMimetype());
        assertEquals(Fields.DESCRIPTION,  TEST_DESC,                            meta.getDescription());
        assertInRange(Fields.UPLOADED_AT, now - 100, now + 100,                 meta.getUploadedAt().longValue());
        assertEquals(Fields.SIZES,        Collections.emptySet(),               meta.getSizes());



        assertTrue("valid", meta.isValid());
    }

    @Test
    public void testToClientMap() throws Exception
    {
        PhotoMetadata meta = new PhotoMetadata(TEST_ID, TEST_USER, TEST_FILE, TEST_MIME, TEST_DESC, TEST_TIMESTAMP, TEST_SIZES_STR);
        Map<String,Object> created = meta.toClientMap();

        assertEquals(Fields.ID,           TEST_ID,                      created.get(Fields.ID));
        assertEquals(Fields.USERNAME,     TEST_USER,                    created.get(Fields.USERNAME));
        assertEquals(Fields.FILENAME,     TEST_FILE,                    created.get(Fields.FILENAME));
        assertEquals(Fields.MIMETYPE,     TEST_MIME,                    created.get(Fields.MIMETYPE));
        assertEquals(Fields.DESCRIPTION,  TEST_DESC,                    created.get(Fields.DESCRIPTION));
        assertEquals(Fields.UPLOADED_AT,  Long.valueOf(TEST_TIMESTAMP), created.get(Fields.UPLOADED_AT));

        List<Map<String,Object>> sizes = (List<Map<String,Object>>)created.get(Fields.SIZES);

        assertEquals("number of sizes", 2, sizes.size());

        // whitebox: since sizes is an EnumSet, we know that order will be order of enum declaration
        assertEquals("size 0 name",         Sizes.ORIGINAL.name(),  sizes.get(0).get("name"));
        assertEquals("size 0 width",        Integer.valueOf(-1),    sizes.get(0).get("width"));
        assertEquals("size 0 height",       Integer.valueOf(-1),    sizes.get(0).get("height"));
        assertEquals("size 0 description",  "original",             sizes.get(0).get("description"));

        // if we get a correct sub-object for one size, there's no reason to think we won't for another
        // so we'll only check that we got the proper size here
        assertEquals("size 1 name",         Sizes.W1024H768.name(), sizes.get(1).get("name"));
    }


    @Test
    public void testDynamoItem() throws Exception
    {
        Item src = new Item()
                    .withString(Fields.ID, TEST_ID)
                    .withString(Fields.USERNAME, TEST_USER)
                    .withString(Fields.FILENAME, TEST_FILE)
                    .withString(Fields.MIMETYPE, TEST_MIME)
                    .withString(Fields.DESCRIPTION, TEST_DESC)
                    .withLong(Fields.UPLOADED_AT, TEST_TIMESTAMP)
                    .withStringSet(Fields.SIZES, TEST_SIZES_STR);

        PhotoMetadata meta = PhotoMetadata.fromDynamoItem(src);

        assertEquals(Fields.ID,           TEST_ID,                      meta.getId());
        assertEquals(Fields.USERNAME,     TEST_USER,                    meta.getUser());
        assertEquals(Fields.FILENAME,     TEST_FILE,                    meta.getFilename());
        assertEquals(Fields.MIMETYPE,     TEST_MIME,                    meta.getMimetype());
        assertEquals(Fields.DESCRIPTION,  TEST_DESC,                    meta.getDescription());
        assertEquals(Fields.UPLOADED_AT,  Long.valueOf(TEST_TIMESTAMP), meta.getUploadedAt());
        assertEquals(Fields.SIZES,        TEST_SIZES,                   meta.getSizes());

        assertTrue("valid", meta.isValid());

        Item dst = meta.toDynamoItem();

        assertEquals(Fields.ID,           TEST_ID,                      dst.getString(Fields.ID));
        assertEquals(Fields.USERNAME,     TEST_USER,                    dst.getString(Fields.USERNAME));
        assertEquals(Fields.FILENAME,     TEST_FILE,                    dst.getString(Fields.FILENAME));
        assertEquals(Fields.MIMETYPE,     TEST_MIME,                    dst.getString(Fields.MIMETYPE));
        assertEquals(Fields.DESCRIPTION,  TEST_DESC,                    dst.getString(Fields.DESCRIPTION));
        assertEquals(Fields.UPLOADED_AT,  TEST_TIMESTAMP,               dst.getLong(Fields.UPLOADED_AT));
        assertEquals(Fields.SIZES,        TEST_SIZES_STR,               dst.getStringSet(Fields.SIZES));
    }
}
