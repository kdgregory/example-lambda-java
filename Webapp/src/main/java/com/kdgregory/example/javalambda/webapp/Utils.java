// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

import java.util.HashMap;
import java.util.Map;

import net.sf.kdgcommons.collections.CollectionUtil;
import net.sf.kdgcommons.lang.StringUtil;

/**
 *  Holds utility functions that would otherwise be replicated in service classes.
 */
public class Utils
{
    /**
     *  Constructs a Set-Cookie header for a session cookie. Returns null if the
     *  passed value is empty/null (this allows use with conditional
     *  <p>
     *  Intended for the access
     *  and refresh tokens, and returns null if the passed value is empty/null.
     */
    private static String createSetCookie(String cookieName, String cookieValue)
    {
        if (StringUtil.isBlank(cookieValue))
            return null;
        else
            return cookieName + "=" + cookieValue + "; Path=/; HttpOnly";
    }


    /**
     *  Constructs the response map. This should be called by all service methods, rather
     *  than attempting to build the response themselves.
     *
     *  @param  request         The original request, which may have been modified by the
     *                          dispatcher (eg, to add tokens or other non-method-specific
     *                          information).
     *  @param  responseCode    An enum that's used to populate the response code and
     *                          description fields.
     *  @param  data            The action-specific-specific response body. May be any type,
     *                          or null.
     */
    public static Map<String,Object> buildResponse(
            Map<String,Object> request,
            Constants.ResponseCodes responseCode,
            Object data)
    {
        Map<String,Object> result = new HashMap<String,Object>();
        result.put(Constants.CommonResponseFields.RESPONSE_CODE, responseCode.toString());
        result.put(Constants.CommonResponseFields.DESCRIPTION, responseCode.getDescription());
        result.put(Constants.CommonResponseFields.DATA, data);
        CollectionUtil.putIfNotNull(result,
                                    Constants.CommonResponseFields.ACCESS_TOKEN_HEADER,
                                    createSetCookie(Constants.Cookies.ACCESS_TOKEN,
                                                    (String)request.get(Constants.CommonRequestFields.ACCESS_TOKEN)));
        CollectionUtil.putIfNotNull(result,
                                    Constants.CommonResponseFields.REFRESH_TOKEN_HEADER,
                                    createSetCookie(Constants.Cookies.REFRESH_TOKEN,
                                                    (String)request.get(Constants.CommonRequestFields.REFRESH_TOKEN)));
        return result;
    }
}
