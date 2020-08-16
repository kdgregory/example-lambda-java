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
    INVALID_REQUEST         ("The request is missing needed fields");


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