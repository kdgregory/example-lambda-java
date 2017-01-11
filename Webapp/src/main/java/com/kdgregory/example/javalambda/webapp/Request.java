// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

import java.util.Map;


/**
 *  Holds all of the data extracted from the original request. It will be passed
 *  to each of the service methods.
 */
public class Request
{
    private String             action;
    private Tokens             tokens;
    private Map<String,Object> body;


    /**
     *  @param action   The request's action.
     *  @param tokens   Authentication tokens provided with the request (name => value).
     *  @param body     The request body; may be an empty map.
     */
    public Request(String action, Tokens tokens, Map<String,Object> body)
    {
        this.action = action;
        this.tokens = tokens;
        this.body = body;
    }


    public String getAction()
    {
        return action;
    }


    public Tokens getTokens()
    {
        return tokens;
    }


    public Map<String,Object> getBody()
    {
        return body;
    }

}
