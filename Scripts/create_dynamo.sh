#!/bin/bash
#
# Creates the DynamoDB table that holds image metadata.
#
#   create-dynamo.sh TABLE_NAME
#

cat > /tmp/$$-tabledef.json <<EOF
{
    "AttributeDefinitions": [
        {
            "AttributeName": "username", 
            "AttributeType": "S"
        },
        {
            "AttributeName": "id", 
            "AttributeType": "S"
        }
    ], 
    "KeySchema": [
        {
            "AttributeName": "username", 
            "KeyType": "HASH"
        },
        {
            "AttributeName": "id", 
            "KeyType": "RANGE"
        }
    ], 
    "ProvisionedThroughput": {
        "ReadCapacityUnits": 5, 
        "WriteCapacityUnits": 5
    }
}
EOF

aws dynamodb create-table --table-name $1 --cli-input-json file:///tmp/$$-tabledef.json > /tmp/$$-tabledef-output.json
