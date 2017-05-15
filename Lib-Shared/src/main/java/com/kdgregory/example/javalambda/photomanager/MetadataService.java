// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.photomanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;

import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoKey;
import com.kdgregory.example.javalambda.photomanager.tabledef.PhotoMetadata;
import com.kdgregory.example.javalambda.photomanager.util.DynamoHelper;


/**
 *  This service supports retrieval and update of photo metadata.
 */
public class MetadataService
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonDynamoDBClient ddbClient;
    private String ddbTableName;


    public MetadataService(String ddbTableName)
    {
        ddbClient = new AmazonDynamoDBClient();
        this.ddbTableName = ddbTableName;
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Retrieves the list of photos matching the supplied key. May be none,
     *  one, or many (if the key only specifies user).
     */
    public List<PhotoMetadata> retrieve(PhotoKey key)
    {
        logger.debug("retrieving metadata for user {} / id {}", key.getUserId(), key.getPhotoId());

        List<PhotoMetadata> result = new ArrayList<PhotoMetadata>();

        QueryRequest request = new QueryRequest()
                               .withTableName(ddbTableName)
                               .withKeyConditionExpression(DynamoHelper.queryExpression(key))
                               .withExpressionAttributeValues(DynamoHelper.queryValues(key));

        boolean more = false;
        do
        {
            QueryResult response = ddbClient.query(request);
            logger.debug("retrieved {} items in batch", response.getCount());
            for (Map<String,AttributeValue> item : response.getItems())
            {
                result.add(PhotoMetadata.fromDynamoMap(item));
            }

            // the documentation says this will be empty if there are no more records to retrieve,
            // but in reality it's null ... so I'll handle both
            Map<String,AttributeValue> lastEvaluatedKey = response.getLastEvaluatedKey();
            more = (lastEvaluatedKey != null) && (lastEvaluatedKey.size() > 0);
            request.setExclusiveStartKey(lastEvaluatedKey);
        } while (more);

        logger.debug("retrieved {} items total", result.size());
        return result;
    }


    /**
     *  Stores the provided metadata.
     *
     *  @return flag indicating whether or not the metadata could be stored.
     */
    public boolean store(PhotoMetadata metadata)
    {
        if (!metadata.isValid())
        {
            logger.warn("upload called with invalid metadata: " + metadata);
            return false;
        }

        logger.debug("storing metadata for user {} / id {}", metadata.getUser(), metadata.getId());
        ddbClient.putItem(ddbTableName, metadata.toDynamoMap());
        return true;
    }
}
