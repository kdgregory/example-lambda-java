// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.messages;

import net.sf.kdgcommons.lang.StringUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *  This message is sent to the Resizer whenever a new photo is uploaded.
 */
public class NewPhoto
{
    private String userId;
    private String photoId;

    @JsonCreator
    public NewPhoto(
        @JsonProperty("userId") String userId,
        @JsonProperty("photoId") String photoId)
    {
        if (StringUtil.isBlank(userId))
            throw new IllegalArgumentException("blank userId");
        if (StringUtil.isBlank(photoId))
            throw new IllegalArgumentException("blank photoId");

        this.userId = userId;
        this.photoId = photoId;
    }

//----------------------------------------------------------------------------
//  Accessors
//----------------------------------------------------------------------------

    @JsonProperty("userId")
    public String getUserId()
    {
        return userId;
    }


    @JsonProperty("photoId")
    public String getPhotoId()
    {
        return photoId;
    }
}
