// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.webapp.services;


/**
 *  This is a marker exception, thrown by a service when it has tried and failled
 *  to handle an internal exception and failed. The service is assumed to have
 *  logged the underlying exception, along with any context, so the Dispatcher
 *  has no need to do any additional logging; it simply returns a 500.
 */
public class UnhandledServiceException extends RuntimeException
{
    private static final long serialVersionUID = 1L;


    public UnhandledServiceException()
    {
        // nothing here
    }
}
