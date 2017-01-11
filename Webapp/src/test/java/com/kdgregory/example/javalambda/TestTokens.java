// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda;

import org.junit.Test;
import static org.junit.Assert.*;

import net.sf.kdgcommons.collections.CollectionUtil;

import com.kdgregory.example.javalambda.webapp.Tokens;


public class TestTokens
{
    @Test
    public void testConstructFromHeader() throws Exception
    {
        Tokens t1 = new Tokens("ACCESS_TOKEN=test1; REFRESH_TOKEN=test2");
        assertEquals("accessToken",  "test1", t1.getAccessToken());
        assertEquals("refreshToken", "test2", t1.getRefreshToken());

        Tokens t2 = new Tokens("ACCESS_TOKEN=test1");
        assertEquals("accessToken",  "test1", t2.getAccessToken());
        assertEquals("refreshToken", null,    t2.getRefreshToken());

        Tokens t3 = new Tokens("REFRESH_TOKEN=test2");
        assertEquals("accessToken",  null,    t3.getAccessToken());
        assertEquals("refreshToken", "test2", t3.getRefreshToken());

        Tokens t4 = new Tokens("");
        assertEquals("accessToken",  null,    t4.getAccessToken());
        assertEquals("refreshToken", null,    t4.getRefreshToken());

        Tokens t5 = new Tokens(null);
        assertEquals("accessToken",  null,    t5.getAccessToken());
        assertEquals("refreshToken", null,    t5.getRefreshToken());
    }


    @Test
    public void testCreateHeader() throws Exception
    {
        Tokens t1 = new Tokens("test1", "test2");
        assertEquals(CollectionUtil.asMap(
                        "Set-Cookie", "ACCESS_TOKEN=test1; Path=/; HttpOnly",
                        "Set-COOKIE", "REFRESH_TOKEN=test2; Path=/; HttpOnly"),
                     t1.createCookieHeaders());

        Tokens t2 = new Tokens(null, "test2");
        assertEquals(CollectionUtil.asMap(
                        "Set-COOKIE", "REFRESH_TOKEN=test2; Path=/; HttpOnly"),
                     t2.createCookieHeaders());

        Tokens t3 = new Tokens("test1", null);
        assertEquals(CollectionUtil.asMap(
                        "Set-Cookie", "ACCESS_TOKEN=test1; Path=/; HttpOnly"),
                     t3.createCookieHeaders());

        Tokens t4 = new Tokens(null, null);
        assertEquals(CollectionUtil.asMap(),
                     t4.createCookieHeaders());
    }
}
