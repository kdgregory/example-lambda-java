// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp;

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