// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    public Map<String,Object> handler(Map<String,Object> proxyRequest, Context lambdaContext)
    {
        try
        {
            // this is the only place we pull pieces out of the AWS request so forego constants
            Request request = extractRequest(proxyRequest);
            Response response = dispatch(request);
            return buildResponseMap(response);
        }
        catch (IllegalArgumentException ex)
        {
            logger.warn("invalid client request", ex.getMessage());
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
    private Request extractRequest(Map<String,Object> proxyRequest)
    {
        // I'm retrieving the raw data all at once, so that I don't sprinkle access
        // to the original request throughout the method; I'm using literals because
        // this is the only place that these values will appear

        String method      = (String)CollectionUtil.getVia(proxyRequest, "httpMethod");
        String action      = (String)CollectionUtil.getVia(proxyRequest, "pathParameters", "action");
        String tokenHeader = (String)CollectionUtil.getVia(proxyRequest, "headers", "Cookie");
        String body        = (String)CollectionUtil.getVia(proxyRequest, "body");

        // body will be empty on GET, but rather than have separate code paths I'll give a dummy value
        if (StringUtil.isEmpty(body))
        {
            body = "{}";
        }

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
     *  to throw any runtime exception, and IllegalArgumentException is handled as
     *  a special case (it's turned into a 400).
     */
    private Response dispatch(Request request)
    {
        logger.info("dispatching action: {} {}", request.getMethod(), request.getAction());
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
            case RequestActions.UPLOAD :
                return invokeIf(request, HttpMethod.POST, authorized(r -> photoService.upload(r)));
            default:
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
        Map<String,String> headers = response.getTokens().createCookieHeaders();
        headers.put("Content-Type", "application/json");

        Map<String,Object> responseMap = new HashMap<>();
        responseMap.put("headers", headers);
        responseMap.put("statusCode", response.getStatusCode());

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

        return responseMap;
    }
}
