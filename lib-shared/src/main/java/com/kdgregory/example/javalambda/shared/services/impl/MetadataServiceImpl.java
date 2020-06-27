// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.shared.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import com.kdgregory.example.javalambda.shared.data.PhotoMetadata;
import com.kdgregory.example.javalambda.shared.data.PhotoMetadata.Fields;
import com.kdgregory.example.javalambda.shared.services.MetadataService;


/**
 *  This service supports retrieval and update of photo metadata.
 */
public class MetadataServiceImpl implements MetadataService
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private DynamoDB ddbClient;
    private Table metadataTable;
    private Index photoIndex;


    public MetadataServiceImpl(String ddbTableName)
    {
        AmazonDynamoDB lowLevelClient = AmazonDynamoDBClientBuilder.defaultClient();
        ddbClient = new DynamoDB(lowLevelClient);
        metadataTable = ddbClient.getTable(ddbTableName);
        photoIndex = metadataTable.getIndex("byID");
    }

//----------------------------------------------------------------------------
//  Implementation of MetadataService
//----------------------------------------------------------------------------

    /**
     *  Stores the provided metadata.
     *
     *  @return flag indicating whether or not the metadata could be stored.
     */
    @Override
    public boolean store(PhotoMetadata metadata)
    {
        logger.debug("storing metadata for user {}, photo {}", metadata.getUser(), metadata.getId());
        if (!metadata.isValid())
        {
            logger.warn("store called with invalid metadata: {}", metadata);
            return false;
        }

        metadataTable.putItem(metadata.toDynamoItem());
        return true;
    }


    /**
     *  Retrieves a photo by its ID. Returns null if unable to find the photo.
     */
    @Override
    public PhotoMetadata retrieve(String photoId)
    {
        logger.debug("retrieving metadata for photo {}", photoId);

        String username = retrieveUsername(photoId);
        if (username == null)
            return null;

        Item item = metadataTable.getItem(Fields.USERNAME, username, Fields.ID, photoId);
        return (item != null)
             ? PhotoMetadata.fromDynamoItem(item)
             : null;
    }


    /**
     *  Retrieves all photos for a given user.
     */
    @Override
    public List<PhotoMetadata> retrieveByUser(String username)
    {
        logger.debug("retrieving metadata for all photos belonging to user {}", username);

        List<PhotoMetadata> result = new ArrayList<>();
        for (Item item : metadataTable.query(Fields.USERNAME, username))
        {
            result.add(PhotoMetadata.fromDynamoItem(item));
        }
        Collections.sort(result);
        return result;
    }


    /**
     *  Deletes the metadata for the specified photo, if it exists. This is intended
     *  primarily to support the integration tests.
     */
    @Override
    public void delete(String photoId)
    {
        logger.debug("deleting metadata for photo {}", photoId);

        String username = retrieveUsername(photoId);
        if (username != null)
        {
            metadataTable.deleteItem(Fields.USERNAME, username, Fields.ID, photoId);
        }
    }

//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    /**
     *  Queryies the GSI to retrieve the username for a given photo. Returns
     *  null if the photo doesn't exist.
     */
    private String retrieveUsername(String photoId)
    {
        Iterator<Item> indexResult = photoIndex.query(Fields.ID, photoId).iterator();
        return (indexResult.hasNext())
             ? indexResult.next().getString(Fields.USERNAME)
             : null;
    }
}
