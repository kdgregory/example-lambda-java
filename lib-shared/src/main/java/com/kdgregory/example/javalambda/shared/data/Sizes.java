// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.data;

import java.util.HashMap;
import java.util.Map;

/**
 *  Defines all supported sizes, along with information about those sizes.
 *  The enum name is used in the S3 key for the image.
 */
public enum Sizes
{
    ORIGINAL    (  -1,  -1,     "original"),
    THUMB       ( 180,  180,    "thumbnail"),
    W1024H768   (1024,  768,    "1024 x 768"),
    W640H480    ( 640,  480,    "640 x 480");


//----------------------------------------------------------------------------
//  Attribbutes and constructors
//----------------------------------------------------------------------------

    private int width;
    private int height;
    private String description;

    private Sizes(int width, int height, String description)
    {
        this.width = width;
        this.height = height;
        this.description = description;
    }


    public int getWidth()
    {
        return width;
    }


    public int getHeight()
    {
        return height;
    }


    public String getDescription()
    {
        return description;
    }

//----------------------------------------------------------------------------
//  Useful methods
//----------------------------------------------------------------------------

    /**
     *  Transforms this enum into a map of values for sending to client.
     */
    public Map<String,Object> toMap()
    {
        Map<String,Object> map = new HashMap<>();
        map.put("name", name());
        map.put("description", description);
        map.put("width", width);
        map.put("height", height);
        return map;
    }
}