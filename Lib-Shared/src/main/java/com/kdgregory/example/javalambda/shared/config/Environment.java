// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.config;

import net.sf.kdgcommons.lang.StringUtil;


/**
 *  Defines the environment variables known to this application, along with a method
 *  that retrieves them or throws if they aren't defined.
 */
public class Environment
{
    public final static String  COGNITO_POOL_ID     = "COGNITO_POOL_ID";
    public final static String  COGNITO_CLIENT_ID   = "COGNITO_CLIENT_ID";
    public final static String  DYNAMO_TABLE        = "DYNAMO_TABLE";
    public final static String  SNS_TOPIC_ARN       = "SNS_TOPIC_ARN";
    public final static String  S3_UPLOAD_BUCKET    = "S3_UPLOAD_BUCKET";
    public final static String  S3_IMAGE_BUCKET     = "S3_IMAGE_BUCKET";


    public static String getOrThrow(String varname)
    {
        String value = System.getenv(varname);
        if (StringUtil.isBlank(value))
            throw new IllegalArgumentException("unset environment variable: " + varname);
        return value;
    }
}
