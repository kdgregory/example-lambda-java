#!/bin/bash
################################################################################################
#
# Creates all Lambda functions and the API Gateway, using a CloudFormation script (this script
# exists to avoid the pain of specifying CloudFormation parameters).
#
#   create_lambda.sh BASE_NAME BUCKET_NAME POOL_ID CLIENT_ID
#
#       BASE_NAME   is the name for this deployment. All AWS objects created by this script
#                   are named by adding suffixes to this base
#       BUCKET_NAME is the name of the bucket used for long-term app storage (including
#                   deployed code)
#       POOL_ID     is the ID of the Cognito user pool for this app
#       CLIENT_ID   is the "app" ID used to access the Cognito user pool
#
# You must run this script from the root directory of the project. Prior to running this
# script you must have run create_bucket.sh to create and populate the application's S3
# bucket, create_pool.sh to create the Cognito user pool, and create_infra.sh to create
# the database and SQS queues.
#
################################################################################################

set -e

. Scripts/_common.sh

BASE_NAME=$1
BUCKET_NAME=$2
COGNITO_POOL_ID=$3
COGNITO_CLIENT_ID=$4


cat > ${TMPDIR}/create_lambda_params.json <<EOF
[
  {
    "ParameterKey":     "BaseName",
    "ParameterValue":   "${BASE_NAME}"
  },
  {
    "ParameterKey":     "Bucket",
    "ParameterValue":   "${BUCKET_NAME}"
  },
  {
    "ParameterKey":     "WebappJar",
    "ParameterValue":   "${DEPLOYMENT_PREFIX}/${WEBAPP_FILE}"
  },
  {
    "ParameterKey":     "CognitoPoolId",
    "ParameterValue":   "${COGNITO_POOL_ID}"
  },
  {
    "ParameterKey":     "CognitoClientId",
    "ParameterValue":   "${COGNITO_CLIENT_ID}"
  }
]
EOF

aws cloudformation create-stack \
                   --stack-name ${BASE_NAME}-Lambda \
                   --template-body file://Scripts/create_lambda.cf \
                   --capabilities CAPABILITY_NAMED_IAM \
                   --parameters "$(< ${TMPDIR}/create_lambda_params.json)"

