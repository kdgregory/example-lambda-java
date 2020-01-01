// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.util;


/**
 *  All response codes and their descriptions. Note that codes may not
 *  be applicable to different functions.
 */
public enum ResponseCodes
{
    SUCCESS                 (""),
    INTERNAL_ERROR          ("An internal error occurred; this has been logged"),
    INVALID_OPERATION       ("Unsupported operation"),
    INVALID_REQUEST         ("The request is missing needed fields"),
    NOT_AUTHENTICATED       ("The user did not provide valid authentication tokens"),
    INVALID_USER            ("Incorrect username or password"),
    TEMPORARY_PASSWORD      ("Attempted to login with temporary password; need to confirm signup"),
    USER_CREATED            ("User creation initiated; move on to confirmation"),
    USER_ALREADY_EXISTS     ("A user with this email address already exists"),
    INVALID_PASSWORD        ("Password does not meeet validation requirements (mix of numbers, upper and lower case letters)");


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