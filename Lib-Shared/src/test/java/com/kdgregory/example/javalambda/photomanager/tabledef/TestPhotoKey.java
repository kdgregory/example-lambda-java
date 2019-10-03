// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager.tabledef;

import org.junit.Test;
import static org.junit.Assert.*;

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
}
