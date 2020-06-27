// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.kdgregory.example.javalambda.webapp.services.PhotoService;
import com.kdgregory.example.javalambda.webapp.services.UnhandledServiceException;
import com.kdgregory.example.javalambda.webapp.services.UserService;
import com.kdgregory.example.javalambda.webapp.util.Request;
import com.kdgregory.example.javalambda.webapp.util.RequestActions;
import com.kdgregory.example.javalambda.webapp.util.Response;
import com.kdgregory.example.javalambda.webapp.util.ResponseCodes;
import com.kdgregory.example.javalambda.webapp.util.Tokens;
import com.kdgregory.example.javalambda.webapp.util.Request.HttpMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import net.sf.kdgcommons.collections.CollectionUtil;
import net.sf.kdgcommons.lang.StringUtil;



/**
 *  Lambda handler: receives the request, extracts the information that we care
 *  about, calls a service method, and then packages the results.
 */
public class Dispatcher
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private ObjectMapper mapper = new ObjectMapper();
    private UserService userService = new UserService();
    private PhotoService photoService = new PhotoService();

    private Pattern actionRegex = Pattern.compile("/api/(.*)");


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    public Map<String,Object> handler(Map<String,Object> albRequest, Context lambdaContext)
    {
        MDC.clear();
        MDC.put("requestId", lambdaContext.getAwsRequestId());

        try
        {
            Request request = extractRequest(albRequest);
            Response response = dispatch(request);
            return buildResponseMap(response);
        }
        catch (IllegalArgumentException ex)
        {
            logger.warn("invalid client request: {}", ex.getMessage());
            return buildResponseMap(new Response(400));
        }
        catch (UnhandledServiceException ignored)
        {
            return buildResponseMap(new Response(500));
        }
        catch (Exception ex)
        {
            logger.error("unexpected exception during request processing", ex);
            return buildResponseMap(new Response(500));
        }
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    /**
     *  Constructs the request object by extracting information from the map
     *  provided by API Gateway.
     *
     *   @throws IllegalArgumentException if unable to process the strings;
     *           should be transformed to a 400 error.
     */
    private Request extractRequest(Map<String,Object> albRequest)
    {
        // I'm retrieving the raw data all at once, so that I don't sprinkle access
        // to the original request throughout the method; I'm using literals because
        // this is the only place that these values will appear

        String method      = (String)CollectionUtil.getVia(albRequest, "httpMethod");
        String path        = (String)CollectionUtil.getVia(albRequest, "path");
        String tokenHeader = (String)CollectionUtil.getVia(albRequest, "headers", "cookie");
        String body        = (String)CollectionUtil.getVia(albRequest, "body");

        logger.info("received {} {}", method, path);

        Matcher actionMatch = actionRegex.matcher(path);
        if (! actionMatch.matches())
        {
            throw new IllegalArgumentException("invalid request path: " + path);
        }
        String action = actionMatch.group(1);

        // body will be empty on GET, but rather than have separate code paths I'll give a dummy value
        // TODO - add defaultIfEmpty() to KDGCommons
        if (StringUtil.isEmpty(body)) body = "{}";

        try
        {
            return new Request(
                    method,
                    action,
                    new Tokens(tokenHeader),
                    CollectionUtil.cast(
                        mapper.readValue(body, HashMap.class),
                        String.class, Object.class));
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException("unable to parse request body: " + ex.getMessage());
        }
    }


    /**
     *  Dispatches based on the request action. This is extracted into a method so
     *  that we can simply return from each switch. Called functions are permitted
     *  to throw any runtime exception; this is handled by caller.
     */
    private Response dispatch(Request request)
    {
        switch (request.getAction())
        {
            case RequestActions.SIGNIN :
                return invokeIf(request, HttpMethod.POST, r -> userService.signIn(r));
            case RequestActions.SIGNUP :
                return invokeIf(request, HttpMethod.POST, r -> userService.signUp(r));
            case RequestActions.CONFIRM_SIGNUP :
                return invokeIf(request, HttpMethod.POST, r -> userService.confirmSignUp(r));
            case RequestActions.LIST :
                return invokeIf(request, HttpMethod.GET,  authorized(r -> photoService.listPhotos(r)));
            case RequestActions.REQUEST_UPLOAD :
                return invokeIf(request, HttpMethod.POST, authorized(r -> photoService.prepareUpload(r)));
            default:
                logger.warn("unknown action, ignoring: {}", request.getAction());
                return new Response(404);
        }
    }


    /**
     *  Verifies that the request matches the desired method, and if yes dispatches
     *  to the provided function.
     */
    private Response invokeIf(Request req, Request.HttpMethod method, Function<Request,Response> f)
    {
        return (req.getMethod() == method)
             ? f.apply(req)
             : new Response(ResponseCodes.INVALID_REQUEST);
    }


    /**
     *  Returns a function that checks authorization before invoking passed function.
     *  This is intended to be passed to {@link invokeIf}, as a semi-DSL.
     */
    private Function<Request,Response> authorized(Function<Request,Response> f)
    {
        return new Function<Request,Response>()
        {
            @Override
            public Response apply(Request req)
            {
                return userService.invokeCheckedOperation(req, f);
            }
        };
    }


    /**
     *  Constructs the map that is sent back to API Gateway.
     *
     *  @param tokens  The current set of authentication tokens.
     *  @param data     The response body, as returned by the service.
     */
    private Map<String,Object> buildResponseMap(Response response)
    {
        Map<String,Object> responseMap = new HashMap<>();
        responseMap.put("statusCode", response.getStatusCode());

        Map<String,String> headers = response.getTokens().createCookieHeaders();
        headers.put("Content-Type", "application/json");
        headers.put("Cache-Control", "no-cache");
        responseMap.put("headers", headers);

        // default to an empty body
        responseMap.put("body", null);
        if (response.getBody() != null)
        {
            try
            {
                responseMap.put("body", mapper.writeValueAsString(response.getBody()));
            }
            catch (JsonProcessingException ex)
            {
                logger.warn("unable to convert response body: " + ex.getMessage());
                responseMap.put("statusCode", 500);
            }
        }
        responseMap.put("isBase64Encoded", Boolean.FALSE);

        return responseMap;
    }
}
