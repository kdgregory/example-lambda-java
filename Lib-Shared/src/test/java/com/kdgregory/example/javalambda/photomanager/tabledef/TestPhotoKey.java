// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager.tabledef;

import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoKey;


public class TestPhotoKey
{
    @Test
    public void testCombinedForm() throws Exception
    {
        String combinedValue = "foo@mailinator.com/123456";

        PhotoKey key = new PhotoKey(combinedValue);
        assertEquals("username", "foo@mailinator.com", key.getUserId());
        assertEquals("photo ID", "123456",             key.getPhotoId());

        assertEquals("combination", combinedValue, key.toCombinedValue());
    }


    @Test
    public void testIsValid() throws Exception
    {
        assertTrue("both components present", new PhotoKey("foo", "bar").isValid());
        assertFalse("missing userid",         new PhotoKey("", "bar").isValid());
        assertFalse("missing photoId",        new PhotoKey("foo", "").isValid());
    }



    @Test
    public void testToDynamoKey() throws Exception
    {
        String expectedUserKey = ":" + Fields.USERNAME;
        String expectedPhotoKey = ":" + Fields.ID;

        String expectedUserValue = "foo";
        String expectedPhotoValue = "bar";

        Map<String,AttributeValue> map1 = new PhotoKey(expectedUserValue, expectedPhotoValue).toDynamoMap();
        assertEquals("map1 size",     2, map1.size());
        assertEquals("map1 username", expectedUserValue, map1.get(expectedUserKey).getS());
        assertEquals("map1 photoid",  expectedPhotoValue, map1.get(expectedPhotoKey).getS());

        Map<String,AttributeValue> map2 = new PhotoKey(expectedUserValue, "").toDynamoMap();
        assertEquals("map2 size",     1,                  map2.size());
        assertEquals("map2 username", expectedUserValue,  map2.get(expectedUserKey).getS());
        assertEquals("map2 photoid",  null,               map2.get(expectedPhotoKey));

        Map<String,AttributeValue> map3 = new PhotoKey("", expectedPhotoValue).toDynamoMap();
        assertEquals("map3 size",     1,                  map3.size());
        assertEquals("map3 username", null,               map3.get(expectedUserKey));
        assertEquals("map3 photoid",  expectedPhotoValue, map3.get(expectedPhotoKey).getS());

        Map<String,AttributeValue> map4 = new PhotoKey(null, null).toDynamoMap();
        assertEquals("map4 size",     0,                  map4.size());
    }
}
