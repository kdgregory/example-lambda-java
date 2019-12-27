// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.resizer;


/**
 *  A wrapper exception for any problems within the resizer.
 */
public class ResizerException
extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private String photoId;

    public ResizerException(String message, String photoId)
    {
        super(message);
        this.photoId = photoId;
    }

    public ResizerException(String message, Throwable wrapped, String photoId)
    {
        super(message, wrapped);
        this.photoId = photoId;
    }

    @Override
    public String getMessage()
    {
        return super.getMessage() + " (photo: " + photoId + ")";
    }
}
