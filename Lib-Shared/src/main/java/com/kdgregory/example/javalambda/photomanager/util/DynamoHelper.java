// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.kdgcommons.lang.StringUtil;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;


/**
 *  Static utility methods for common DynamoDB tasks.
 */
public class DynamoHelper
{
    private final static String KEY_USERNAME = ":username";
    private final static String KEY_PHOTO_ID = ":photo_id";


    /**
     *  Stores the passed value in a Dynamo field map, skipping if the value is
     *  null or empty. Will accept collections of strings, numbers, or enums;
     *  such collections must be homogeneous. Collections of enums are translated
     *  by calling getName() on the values.
     */
    public static void put(Map<String,AttributeValue> map, String key, Object value)
    {
        if (value == null)
        {
            return;
        }
        else if (value instanceof String)
        {
            String s = (String)value;
            if (! StringUtil.isEmpty(s))
                map.put(key, new AttributeValue().withS(s));
        }
        else if (value instanceof Number)
        {
            map.put(key, new AttributeValue().withN(value.toString()));
        }
        else if (value instanceof Collection)
        {
            Collection<Object> c = (Collection<Object>)value;
            if (c.size() == 0)
            {
                return;
            }
            Object sample = c.iterator().next();
            if (sample instanceof String)
            {
                List<String> values = c.stream().map(s -> (String)s).collect(Collectors.toList());
                map.put(key, new AttributeValue().withSS(values));
            }
            else if (sample instanceof Number)
            {
                List<String> values = c.stream().map(n -> n.toString()).collect(Collectors.toList());
                map.put(key, new AttributeValue().withNS(values));
            }
            else if (sample instanceof Enum)
            {
                List<String> values = c.stream().map(e -> ((Enum<?>)e).name()).collect(Collectors.toList());
                map.put(key, new AttributeValue().withSS(values));
            }
            else
            {
                throw new IllegalArgumentException("unsupported value type: " + sample.getClass().getName());
            }
        }
        else
        {
            throw new IllegalArgumentException("unsupported value type: " + value.getClass().getName());
        }
    }


    /**
     *  Retrieves a String value, returning null if the value doesn't exist.
     */
    public static String getS(Map<String,AttributeValue> map, String key)
    {
        AttributeValue av = map.get(key);
        return (av == null) ? null : av.getS();
    }


    /**
     *  Retrieves a numeric value, returning null if the value doesn't exist.
     *  Given our use case, the number will always be a Long.
     */
    public static Long getN(Map<String,AttributeValue> map, String key)
    {
        AttributeValue av = map.get(key);
        return (av == null) ? null : Long.valueOf(av.getN());
    }


    /**
     *  Retrieves a list of strings, returning an empty list of the value doesn't exist.
     */
    public static List<String> getSS(Map<String,AttributeValue> map, String key)
    {
        AttributeValue av = map.get(key);
        return (av == null) ? Collections.emptyList() : av.getSS();
    }


    /**
     *  Retrieves a list of numbers, returning an empty list of the value doesn't exist.
     *  As with {@link getN}, the numeric type is Long.
     */
    public static List<Long> getNS(Map<String,AttributeValue> map, String key)
    {
        AttributeValue av = map.get(key);
        return (av == null) ? Collections.emptyList()
                            : av.getNS().stream().map(s -> Long.valueOf(s)).collect(Collectors.toList());
    }


    /**
     *  Produces an expression string that can be used for a Dynamo query using the
     *  passed key. Use in conjunction with (@link #queryMap}.
     */
    public static String queryExpression(String userId, String photoId)
    {
        StringBuilder sb = new StringBuilder(256);
        if (! StringUtil.isBlank(userId))
        {
            sb.append("username = ").append(KEY_USERNAME);
        }

        if (! StringUtil.isBlank(photoId))
        {
            if (sb.length() > 0)
            {
                sb.append(" AND ");
            }
            sb.append("id = ").append(KEY_PHOTO_ID);
        }

        return sb.toString();
    }


    /**
     *  Produces a map of expression values corresponding to the passed key. Use
     *  in conjunction with {@link #queryExpression}.
     */
    public static Map<String,AttributeValue> queryValues(String userId, String photoId)
    {
        Map<String,AttributeValue> result = new HashMap<>();
        put(result, KEY_USERNAME, userId);
        put(result, KEY_PHOTO_ID, photoId);
        return result;
    }
}
