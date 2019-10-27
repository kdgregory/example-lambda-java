// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.util;


/**
 *  Actions supported by the webapp (provided as the trailing part of the URL).
 */
public class RequestActions
{
    // handled by PhotoService

    public final static String  LIST            = "list";
    public final static String  UPLOAD          = "upload";

    // handled by UserService

    public final static String  SIGNIN          = "signin";
    public final static String  SIGNUP          = "signup";
    public final static String  CONFIRM_SIGNUP  = "confirmSignup";
}