// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.util;

import java.util.HashMap;
import java.util.Map;

import net.sf.kdgcommons.lang.StringUtil;


/**
 *  Holds the access and refresh tokens, and provides functions to parse and produce
 *  the cookie headers that hold these tokens.
 *  <p>
 *  Note: the names of the cookies are maintained here (rather than in Constants), as
 *  there's no reason for other code to know about them.
 */
public class Tokens
{
    public final static String ACCESS_TOKEN_NAME  = "ACCESS_TOKEN";
    public final static String REFRESH_TOKEN_NAME = "REFRESH_TOKEN";
    public final static String COOKIE_FORMAT      = "%s=%s; Path=/; HttpOnly; SameSite=strict";

    private String accessToken;
    private String refreshToken;


    /**
     *  Constructs an empty instance. This is used as a placeholder in the response.
     */
    public Tokens()
    {
        // no need to do anything
    }


    /**
     *  Constructs an instance with explicit token values, either of which may be null.
     */
    public Tokens(String accessToken, String refreshToken)
    {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }


    /**
     *  Constructs an instance from the <code>Cookie</code> header value provided by
     *  API Gateway (all cookies in one header, separated by semi-colons).
     */
    public Tokens(String header)
    {
        if (StringUtil.isBlank(header))
            return;

        // simplest way to construct is to break apart the header without concern for
        // actual token names, then set the values based on what we found

        Map<String,String> tokens = new HashMap<String,String>();
        String[] splitCookies = header.split(";\\s*");
        for (String cookie : splitCookies)
        {
            String[] avPair = cookie.split("\\s*=\\s*");
            tokens.put(avPair[0], avPair[1]);
        }

        accessToken = tokens.get(ACCESS_TOKEN_NAME);
        refreshToken = tokens.get(REFRESH_TOKEN_NAME);
    }


    /**
     *  Returns the access token. May be null.
     */
    public String getAccessToken()
    {
        return accessToken;
    }


    /**
     *  Returns the refresh token. May be null.
     */
    public String getRefreshToken()
    {
        return refreshToken;
    }


    /**
     *  Constructs the of Set-Cookie headers for the access and refresh tokens.
     *  <p>
     *  FIXME: when I first implemented this, API Gateway did not support multi-valued
     *  headers, so I had to use the hack of case-sensitive values. API Gateway added
     *  support in October 2018, and ALB has supported multi-value headers since it
     *  started supporting Lambdas, so this hack can be removed. However, doing so
     *  means updating all of the entire request/response handling code.
     */
    public Map<String,String> createCookieHeaders()
    {
        Map<String,String> result = new HashMap<String,String>();

        if (! StringUtil.isBlank(accessToken))
        {
            result.put("Set-Cookie", String.format(COOKIE_FORMAT, ACCESS_TOKEN_NAME, accessToken));
        }

        if (! StringUtil.isBlank(refreshToken))
        {
            result.put("Set-COOKIE", String.format(COOKIE_FORMAT, REFRESH_TOKEN_NAME, refreshToken));
        }

        return result;
    }


    /**
     *  Used for diagnostic output: prints a substring of each token.
     */
    @Override
    public String toString()
    {
        return "Tokens(" + ACCESS_TOKEN_NAME  + ": " + StringUtil.substr(accessToken, 0, 8) + ", "
                         + REFRESH_TOKEN_NAME + ": " + StringUtil.substr(refreshToken, 0, 8) + ")";
    }
}
