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
            "AttributeName": "id", 
            "AttributeType": "S"
        }
    ], 
    "KeySchema": [
        {
            "AttributeName": "id", 
            "KeyType": "HASH"
        }
    ], 
    "ProvisionedThroughput": {
        "ReadCapacityUnits": 5, 
        "WriteCapacityUnits": 5
    }
}
EOF

aws dynamodb create-table --table-name $1 --cli-input-json file:///tmp/$$-tabledef.json > /tmp/$$-tabledef-output.json
