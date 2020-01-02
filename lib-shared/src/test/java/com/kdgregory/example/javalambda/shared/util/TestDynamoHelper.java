// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.kdgregory.example.javalambda.shared.util.DynamoHelper;


public class TestDynamoHelper
{
    private enum MyEnum { FOO, BAR }

    @Test
    public void testPutAndGet() throws Exception
    {
        Map<String,AttributeValue> map = new HashMap<>();

        DynamoHelper.put(map, "foo", "bar");
        assertEquals("putS", "bar", map.get("foo").getS());
        assertEquals("getS", "bar", DynamoHelper.getS(map, "foo"));

        DynamoHelper.put(map, "foo", 123);
        assertEquals("putN", "123",             map.get("foo").getN());
        assertEquals("getN", Long.valueOf(123), DynamoHelper.getN(map, "foo"));

        List<String> stringList = Arrays.asList("argle", "bargle");
        DynamoHelper.put(map, "foo", stringList);
        assertEquals("putSS", stringList, map.get("foo").getSS());
        assertEquals("getSS", stringList, DynamoHelper.getSS(map, "foo"));

        List<Long> numList = Arrays.asList(Long.valueOf(123), Long.valueOf(456));
        DynamoHelper.put(map, "foo", numList);
        assertEquals("putNS", Arrays.asList("123","456"), map.get("foo").getNS());
        assertEquals("getNS", numList,                    DynamoHelper.getNS(map, "foo"));

        DynamoHelper.put(map, "foo", Arrays.asList(MyEnum.values()));
        assertEquals("putSS", Arrays.asList("FOO","BAR"), map.get("foo").getSS());
        assertEquals("getNS", Arrays.asList("FOO","BAR"), DynamoHelper.getSS(map, "foo"));
    }


    @Test
    public void testQueryHelpersUsernameOnly() throws Exception
    {
        assertEquals("query expression", "username = :username",
                                         DynamoHelper.queryExpression("foo", null));

        Map<String,AttributeValue> queryTerms = DynamoHelper.queryValues("foo", null);
        assertEquals("terms includes user ID",           "foo", queryTerms.get(":username").getS());
        assertEquals("terms does not include photo ID",  null,  queryTerms.get(":photo_id"));
    }


    @Test
    public void testQueryHelpersBothFields() throws Exception
    {
        assertEquals("query expression", "username = :username AND id = :photo_id",
                                         DynamoHelper.queryExpression("foo", "bar"));

        Map<String,AttributeValue> queryTerms = DynamoHelper.queryValues("foo", "bar");
        assertEquals("terms includes user ID",  "foo", queryTerms.get(":username").getS());
        assertEquals("terms includes photo ID", "bar", queryTerms.get(":photo_id").getS());
    }
}
