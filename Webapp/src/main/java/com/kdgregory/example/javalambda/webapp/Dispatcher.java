// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;

import com.kdgregory.example.javalambda.webapp.Constants.ResponseCodes;
import com.kdgregory.example.javalambda.webapp.services.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *  A simple Java handler to verify plumbing.
 */
public class Dispatcher
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private UserService cognitoService = new UserService();


    public Map<String,Object> handler(Map<String,Object> request, Context context)
    {
        logger.debug("request data: {}", request);

        String action = (String)request.get(Constants.CommonRequestFields.ACTION);
        switch (action)
        {
            case Constants.RequestActions.SIGNIN :
                return cognitoService.signIn(request);
            case Constants.RequestActions.SIGNUP :
                return cognitoService.signUp(request);
            case Constants.RequestActions.CONFIRM_SIGNUP :
                return cognitoService.confirmSignUp(request);
            default:
                logger.warn("invalid operation: {}", action);
                return Utils.buildResponse(request, ResponseCodes.INVALID_OPERATION, null);
        }
    }

}
