// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.UUID;

import net.sf.kdgcommons.lang.StringUtil;

import com.kdgregory.example.javalambda.webapp.Request;
import com.kdgregory.example.javalambda.webapp.Response;
import com.kdgregory.example.javalambda.webapp.ResponseCodes;
import com.kdgregory.example.javalambda.webapp.Tokens;

/**
 *  Manages users: sign-up (with confirmation), sign-in, and token-based
 *  authentication.
 */
public class UserService
{
    /**
     *  All expected parameter names, to avoid typos.
     */
    public static class ParamNames
    {
        public final static String  EMAIL               = "email";
        public final static String  PASSWORD            = "password";
        public final static String  TEMPORARY_PASSWORD  = "temporaryPassword";
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------




//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Attempts to sign-in an existing user.
     *  <p>
     *  Expected parameters:
     *  <dl>
     *  <dt> email
     *  <dd> The user's email address, which serves as the account identifier.
     *  <dt> password
     *  <dd> The user's password.
     *  </dl>
     */
    public Response signIn(Request request)
    {
        return new Response(ResponseCodes.SUCCESS, null);
    }


    /**
     *  Initiates signup for a new user.
     *  <p>
     *  Expected parameters:
     *  <dl>
     *  <dt> email
     *  <dd> The user's email address, which serves as the account identifier.
     *  </dl>
     */
    public Response signUp(Request request)
    {
        return new Response(ResponseCodes.SUCCESS, null);
    }


    /**
     *  Confirms signup for a new user.
     *  <p>
     *  Expected parameters:
     *  <dl>
     *  <dt> email
     *  <dd> The user's email address, which serves as the account identifier.
     *  <dt> temporaryPassword
     *  <dd> The temporary password that Cognito sent to the user.
     *  <dt> password
     *  <dd> The permanent password selected by the user.
     *  </dl>
     */
    public Response confirmSignUp(Request request)
    {
        return new Response(ResponseCodes.SUCCESS, null);
    }


    /**
     *  Verifies that a user has logged in, refreshing their access token if required.
     *
     *  Returns a shell response that contains the result of authentication along with
     *  potentially a new set of tokens. Before invoking any operation that requires
     *  authentication, the caller should check the response code; if it's not SUCCESS
     *  return the response to the user. Assuming that the user is authenticated, the
     *  caller should merge the tokens from this response with the response from the
     *  authenticated method.
     */
    public Response authenticate(Request request)
    {
        String accessToken = request.getTokens().getAccessToken();
        String refreshToken = request.getTokens().getRefreshToken();

//        if (StringUtil.isBlank(accessToken) || StringUtil.isBlank(refreshToken))
//            return new Response(ResponseCodes.NOT_AUTHENTICATED);

        // TODO - check authentication and potentially get a new set of tokens
        //        for now I'm going to return a response with a dummy access token

        Tokens tokens = new Tokens(UUID.randomUUID().toString(), null);
        return new Response(tokens);
    }
}
