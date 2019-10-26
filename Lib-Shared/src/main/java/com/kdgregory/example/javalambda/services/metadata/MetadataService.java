// Copyright (c) Keith D Gregory, all rights reserved
package com.kdgregory.example.javalambda.services.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;

import com.kdgregory.example.javalambda.util.DynamoHelper;


/**
 *  This service supports retrieval and update of photo metadata.
 */
public class MetadataService
{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private AmazonDynamoDB ddbClient;
    private String ddbTableName;


    public MetadataService(String ddbTableName)
    {
        ddbClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.ddbTableName = ddbTableName;
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Retrieves the list of photos matching the supplied key. May be none,
     *  one, or many (if the key only specifies user).
     */
    public List<PhotoMetadata> retrieve(String userId, String photoId)
    {
        logger.debug("retrieving metadata for user {}, photo {}", userId, photoId);

        List<PhotoMetadata> result = new ArrayList<PhotoMetadata>();

        QueryRequest request = new QueryRequest()
                               .withTableName(ddbTableName)
                               .withKeyConditionExpression(DynamoHelper.queryExpression(userId, photoId))
                               .withExpressionAttributeValues(DynamoHelper.queryValues(userId, photoId));

        boolean more = false;
        do
        {
            QueryResult response = ddbClient.query(request);
            logger.debug("retrieved {} items in batch", response.getCount());
            for (Map<String,AttributeValue> item : response.getItems())
            {
                try
                {
                    result.add(PhotoMetadata.fromDynamoMap(item));
                }
                catch (Exception ex)
                {
                    logger.error("invalid metadata in Dynamo: {}", item);
                }
            }

            // the documentation says this will be empty if there are no more records to retrieve,
            // but in reality it's null ... so I'll handle both
            Map<String,AttributeValue> lastEvaluatedKey = response.getLastEvaluatedKey();
            more = (lastEvaluatedKey != null) && (lastEvaluatedKey.size() > 0);
            request.setExclusiveStartKey(lastEvaluatedKey);
        } while (more);

        logger.debug("retrieved {} items for photo {}", result.size(), photoId);
        return result;
    }


    /**
     *  Stores the provided metadata.
     *
     *  @return flag indicating whether or not the metadata could be stored.
     */
    public boolean store(PhotoMetadata metadata)
    {
        logger.debug("storing metadata for user {}, photo {}", metadata.getUser(), metadata.getId());
        if (!metadata.isValid())
        {
            logger.warn("upload called with invalid metadata: {}", metadata);
            return false;
        }
        ddbClient.putItem(ddbTableName, metadata.toDynamoMap());
        return true;
    }
}
