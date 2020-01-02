// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services;

import com.kdgregory.example.javalambda.shared.data.Sizes;


/**
 *  This service supports management of a photo's content.
 */
public interface ContentService
{
    /**
     *  Stores the content for a photo at a given size.
     */
    public void store(String photoId, String mimeType, Sizes size, byte[] content);


    /**
     *  Retrieves the content for a photo at a given size, null if unable to find the photo.
     */
    public byte[] retrieve(String photoId, Sizes size);


    /**
     *  Generates a signed URL that can be used for upload of a specified file.
     */
    public String createUploadURL(String filename);


    /**
     *  Moves an uploaded photo from the upload bucket to the image bucket, storing
     *  it as "ORIGINAL" size.
     */
    public void moveUploadToImageBucket(String photoId);
}
