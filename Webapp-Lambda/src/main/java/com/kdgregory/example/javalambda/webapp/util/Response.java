// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.util;

import java.util.HashMap;
import java.util.Map;


/**
 *  Holds the information for the response. This will be returned by all service
 *  methods.
 */
public class Response
{
    /**
     *  Top-level fields in the response body.
     */
    public static class Fields
    {
        /**
         *  The response code.
         */
        public final static String RESPONSE_CODE = "responseCode";

        /**
         *  Text to display to the user for non-successful response.
         */
        public final static String DESCRIPTION = "description";

        /**
         *  The service-specific response data.
         */
        public final static String DATA = "data";
    }


    private int           statusCode = 200;
    private Tokens        tokens     = new Tokens();
    private ResponseCodes responseCode;
    private Object        data;


    /**
     *  Primary constructor for service methods. Assumes status 200.
     *
     *  @param responseCode Service-specific response code with description.
     *  @param data         Service-specific data. Can be any JSON-serializable
     *                      object, or null.
     */
    public Response(ResponseCodes responseCode, Object data)
    {
        this.responseCode = responseCode;
        this.data = data;
    }


    /**
     *  Convenience constructor for service methods that don't return a body.
     *  Assumes status 200.
     *
     *  @param responseCode Service-specific response code with description.
     */
    public Response(ResponseCodes responseCode)
    {
        this(responseCode, null);
    }


    /**
     *  Constructor used by the authentication check when it needs to provide
     *  updated tokens. Status code code defaults to 200, and response code
     *  defaults to SUCCESS.
     */
    public Response(Tokens updatedTokens)
    {
        this(ResponseCodes.SUCCESS);
        this.tokens = updatedTokens;
    }


    /**
     *  Constructor used by authorization wrapper. Attaches updated tokens to
     *  the response.
     */
    public Response(Response wrapped, Tokens updatedTokens)
    {
        this.statusCode = wrapped.statusCode;
        this.tokens = updatedTokens;
        this.responseCode = wrapped.responseCode;
        this.data = wrapped.data;
    }

    /**
     *  Constructor used by dispatcher exception handlers; does not return data.
     *
     *  @param statusCode   The HTTP status to return.
     */
    public Response(int statusCode)
    {
        this.statusCode = statusCode;
    }


    /**
     *  Returns the HTTP status code.
     */
    public int getStatusCode()
    {
        return statusCode;
    }


    /**
     *  Returns the tokens.
     */
    public Tokens getTokens()
    {
        return tokens;
    }


    /**
     *  Returns a response body, based on the provided response code and data.
     *  Will return null if the response code is missing.
     */
    public Map<String,Object> getBody()
    {
        if (responseCode == null)
            return null;

        HashMap<String,Object> body = new HashMap<>();
        body.put(Response.Fields.RESPONSE_CODE, responseCode.name());
        body.put(Response.Fields.DESCRIPTION, responseCode.getDescription());
        body.put(Response.Fields.DATA, data);
        return body;
    }
}
