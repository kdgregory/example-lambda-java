// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager.tabledef;

import net.sf.kdgcommons.lang.StringUtil;

/**
 *  Holds the key used to store photo metadata in Dynamo.
 */
public class PhotoKey
{
    private String userId;
    private String photoId;


    /**
     *  Constructs from discrete components.
     */
    public PhotoKey(String userId, String photoId)
    {
        this.userId = userId;
        this.photoId = photoId;
    }


    /**
     *  Constructs from the USER/PHOTO format used to send messages between
     *  webapp and resizer. Note that either or both components may be blank.
     */
    public PhotoKey(String combinedId)
    {
        this.userId = StringUtil.extractLeft(combinedId, "/");
        this.photoId = StringUtil.extractRightOfLast(combinedId, "/");
    }


//----------------------------------------------------------------------------
//  Accessors -- used mostly for testing
//----------------------------------------------------------------------------

    public String getUserId()
    {
        return userId;
    }


    public String getPhotoId()
    {
        return photoId;
    }


//----------------------------------------------------------------------------
//  Conversions to and from other interesting formats
//----------------------------------------------------------------------------

    /**
     *  Combines the username and photo ID, using a character that shouldn't
     *  appear in either.
     */
    public String toCombinedValue()
    {
        return userId + "/" + photoId;
    }


    /**
     *  Returns a map that can be used for selecting from Dynamo. The keys are
     *  the standard attribute names prefaced with colons (eg: ":username"),
     *  and a null is omitted.
     */



//----------------------------------------------------------------------------
//  Other public methods
//----------------------------------------------------------------------------

    /**
     *  A key is valid only if it has both photo and user IDs.
     */
    public boolean isValid()
    {
        return ! (StringUtil.isBlank(photoId) || StringUtil.isBlank(userId));
    }
}
