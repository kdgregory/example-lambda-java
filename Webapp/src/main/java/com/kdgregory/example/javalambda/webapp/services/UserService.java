// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;

import java.util.Map;

import net.sf.kdgcommons.lang.StringUtil;

import com.kdgregory.example.javalambda.webapp.Constants;
import com.kdgregory.example.javalambda.webapp.Utils;

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
    public Map<String,Object> signIn(Map<String,Object> request)
    {
        return Utils.buildResponse(request, Constants.ResponseCodes.SUCCESS, null);
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
    public Map<String,Object> signUp(Map<String,Object> request)
    {
        return Utils.buildResponse(request, Constants.ResponseCodes.SUCCESS, null);
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
    public Map<String,Object> confirmSignUp(Map<String,Object> request)
    {
        return Utils.buildResponse(request, Constants.ResponseCodes.SUCCESS, null);
    }


    /**
     *  Verifies that a user has logged in, refreshing their access token if required.
     *
     *  Returns a true/false response to indicate authentication, <em>and modifies the
     *  passed request<em> to either (1) remove tokens if the user is authenticated (to
     *  avoid needless Set-Cookie headers) or (2) replace the access token if changed.
     */
    public boolean authenticate(Map<String,Object> request)
    {
        String accessToken = String.valueOf(request.get(Constants.CommonRequestFields.ACCESS_TOKEN));
        String refreshToken = String.valueOf(request.get(Constants.CommonRequestFields.REFRESH_TOKEN));

        if (StringUtil.isBlank(accessToken) || StringUtil.isBlank(refreshToken))
            return false;

        // FIXME - check authentication, remove tokens; for now I want to leave them, to verify
        //         Set-Cookie behavior

        return true;
    }
}
