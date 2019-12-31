// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services;

import java.util.List;

import com.kdgregory.example.javalambda.shared.data.PhotoMetadata;


/**
 *  This service supports retrieval and update of photo metadata.
 */
public interface MetadataService
{
    /**
     *  Stores the provided metadata.
     *
     *  @return flag indicating whether or not the metadata could be stored.
     */
    public boolean store(PhotoMetadata metadata);


    /**
     *  Retrieves a photo by its ID. Returns null if unable to find the photo.
     */
    public PhotoMetadata retrieve(String photoId);


    /**
     *  Retrieves all photos for a given user.
     */
    public List<PhotoMetadata> retrieveByUser(String username);


    /**
     *  Deletes the metadata for the specified photo, if it exists. This is intended
     *  primarily to support the integration tests.
     */
    public void delete(String photoId);
}
