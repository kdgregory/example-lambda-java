// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

/**
 *  Common constants for requests and responses.
 */
public class Constants
{
    /**
     *  Cookie names.
     */
    public static class Cookies
    {
        public final static String ACCESS_TOKEN = "ACCESS_TOKEN";
        public final static String REFRESH_TOKEN = "REFRESH_TOKEN";
    }


    /**
     *  Top-level request elements.
     */
    public static class CommonRequestFields
    {
        /**
         *  The requested action (nominally extracted from the URL).
         */
        public final static String ACTION = "action";

        /**
         *  The access token.
         */
        public final static String ACCESS_TOKEN = "accessToken";

        /**
         *  The refresh token.
         */
        public final static String REFRESH_TOKEN = "refreshToken";

        /**
         *  The service-specific request body.
         */
        public final static String BODY = "body";
    }


    /**
     *  Supported actions.
     */
    public static class RequestActions
    {
        public final static String  SIGNIN          = "signin";
        public final static String  SIGNUP          = "signup";
        public final static String  CONFIRM_SIGNUP  = "confirmSignup";
    }


    /**
     *  Top-level response elements.
     */
    public static class CommonResponseFields
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
         *  The Set-Cookie header value for the access token, if updated.
         */
        public final static String ACCESS_TOKEN_HEADER = "accessTokenHeader";

        /**
         *  The Set-Cookie header value for the refresh token, if updated.
         */
        public final static String REFRESH_TOKEN_HEADER = "refreshTokenHeader";

        /**
         *  The service-specific response data.
         */
        public final static String DATA = "data";
    }


    /**
     *  All response codes and their descriptions. Note that codes may not
     *  be applicable to different functions.
     */
    public enum ResponseCodes
    {
        SUCCESS             (""),
        INVALID_OPERATION   ("Unsupported operation"),
        NOT_AUTHENTICATED   ("The user did not provide valid authentication tokens");

        private String description;

        private ResponseCodes(String description)
        {
            this.description = description;
        }

        public String getDescription()
        {
            return description;
        }
    }
}
