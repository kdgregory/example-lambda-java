// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager.tabledef;

/**
 *  Names for all metadata fields. These are used both as Dynamo attribute names
 *  and for communication between client and server.
 *  <p>
 *  This class will evolve into an enum.
 */
public class Fields
{
    public final static String  ID          = "id";
    public final static String  USERNAME    = "username";
    public final static String  FILENAME    = "filename";
    public final static String  DESCRIPTION = "description";
    public final static String  MIMETYPE    = "mimetype";
    public final static String  UPLOADED_AT = "uploadedAt";
    public final static String  SIZES       = "sizes";

}