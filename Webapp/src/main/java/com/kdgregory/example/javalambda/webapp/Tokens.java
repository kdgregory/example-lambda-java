// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

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
    private final static String ACCESS_TOKEN = "ACCESS_TOKEN";
    private final static String REFRESH_TOKEN = "REFRESH_TOKEN";

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

        accessToken = tokens.get(ACCESS_TOKEN);
        refreshToken = tokens.get(REFRESH_TOKEN);
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
     *  Since these are stored in a map (because Lambda wants to transform it to
     *  JSON), we need to pick distinct header names for each cookie. Luckily,
     *  capitalization works (ie, API Gateway doesn't canonicalize headers).
     */
    public Map<String,String> createCookieHeaders()
    {
        String format = "%s=%s; Path=/; HttpOnly";

        Map<String,String> result = new HashMap<String,String>();

        if (! StringUtil.isBlank(accessToken))
        {
            result.put("Set-Cookie", String.format(format, ACCESS_TOKEN, accessToken));
        }

        if (! StringUtil.isBlank(refreshToken))
        {
            result.put("Set-COOKIE", String.format(format, REFRESH_TOKEN, refreshToken));
        }

        return result;
    }
}
