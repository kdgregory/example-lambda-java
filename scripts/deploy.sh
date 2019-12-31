#!/bin/bash
################################################################################################
#
# Deploys the demo app
#
#   scripts/deploy.sh BASENAME BASE_BUCKETNAME VPC_ID PUBLIC_SUBNETS
#
# This will build the Lambdas, create the application buckets, copy the relevant content into
# those buckets, and launch a CloudFormation stack to build out the rest of the deployment.
#
# To run, you must have your credentials and region configured.
#
################################################################################################

BASENAME=$1
BASE_BUCKETNAME=$2
VPC_ID=$3
PUBLIC_SUBNETS=$4

DEPLOYMENT_BUCKET=${BASE_BUCKETNAME}-deployment
STATIC_BUCKET=${BASE_BUCKETNAME}-static
IMAGES_BUCKET=${BASE_BUCKETNAME}-images
UPLOADS_BUCKET=${BASE_BUCKETNAME}-uploads
LOGS_BUCKET=${BASE_BUCKETNAME}-logs

################################################################################

mvn clean install

################################################################################

echo ""
echo "creating static and deployment buckets"

aws s3 mb s3://$DEPLOYMENT_BUCKET
aws s3 mb s3://$STATIC_BUCKET

################################################################################

echo ""
echo "uploading deployment bundles and static content"

WEBAPP_PATH=(webapp-lambda/target/webapp-lambda-*.jar)
WEBAPP_FILE=$(basename "${WEBAPP_PATH}")
aws s3 cp ${WEBAPP_PATH} s3://${DEPLOYMENT_BUCKET}/${WEBAPP_FILE}

RESIZER_PATH=(resizer-lambda/target/resizer-lambda-*.jar)
RESIZER_FILE=$(basename "${RESIZER_PATH}")
aws s3 cp ${RESIZER_PATH} s3://${DEPLOYMENT_BUCKET}/${RESIZER_FILE}

pushd webapp-static
aws s3 cp . s3://${STATIC_BUCKET}/ --recursive
popd

################################################################################

echo ""
echo "creating CloudFormation stack"


cat > /tmp/cfparams.json <<EOF
[
  {
    "ParameterKey":     "BaseName",
    "ParameterValue":   "${BASENAME}"
  },
  {
    "ParameterKey":     "VpcId",
    "ParameterValue":   "${VPC_ID}"
  },
  {
    "ParameterKey":     "PublicSubnetIds",
    "ParameterValue":   "${PUBLIC_SUBNETS}"
  },
  {
    "ParameterKey":     "DeploymentBucketName",
    "ParameterValue":   "${DEPLOYMENT_BUCKET}"
  },
  {
    "ParameterKey":     "StaticBucketName",
    "ParameterValue":   "${STATIC_BUCKET}"
  },
  {
    "ParameterKey":     "ImageBucketName",
    "ParameterValue":   "${IMAGES_BUCKET}"
  },
  {
    "ParameterKey":     "UploadBucketName",
    "ParameterValue":   "${UPLOADS_BUCKET}"
  },
  {
    "ParameterKey":     "LogsBucketName",
    "ParameterValue":   "${LOGS_BUCKET}"
  },
  {
    "ParameterKey":     "WebappJar",
    "ParameterValue":   "${WEBAPP_FILE}"
  },
  {
    "ParameterKey":     "ResizerJar",
    "ParameterValue":   "${RESIZER_FILE}"
  }
]
EOF

aws cloudformation create-stack \
                   --stack-name ${BASENAME} \
                   --template-body file://scripts/cloudformation.yml \
                   --capabilities CAPABILITY_NAMED_IAM \
                   --parameters "$(< /tmp/cfparams.json)"
