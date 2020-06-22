#!/bin/bash
################################################################################################
#
# Deploys the demo app: builds the Lambdas, creates the application buckets, copies static
# content into those buckets, and launches a CloudFormation stack to build out the rest of
# the deployment.
#
# Invocation:
#
#   scripts/deploy.sh BASENAME BASE_BUCKETNAME VPC_ID PUBLIC_SUBNETS [DNS_DOMAIN HOSTNAME ACM_CERT_ARN]
#
# Where:
#
#   BASENAME        - Used for the CloudFormation stack name, and also as a prefix for any
#                     resources created by the stack (other than buckets).
#   BASE_BUCKETNAME - A prefix for all buckets created by the stack.
#   VPC_ID          - ID of a VPC in the current account.
#   PUBLIC_SUBNETS  - Comma-separated list of public subnets in the current account, used to
#                     deploy ALB.
#   DNS_DOMAIN      - (optional) DNS domain that corresponds to a hosted zone managed by the
#                     current account (do not include trailing dot).
#   HOSTNAME        - (optional) Custom hostname that will be used to access application.
#   ACM_CERT_ARN    - (optional) ACM certificate valid for the specified host.domain.
#
# To run, you must have your credentials and region configured.
#
# If you want a custom hostname, you must specify all three of the optional parameters. If you
# omit them, you will need to access the application using the CloudFront assigned ID.
#
################################################################################################

if [[ $# -ne 4 && $# -ne 7 ]] ; then
    echo "invocation: scripts/deploy.sh BASENAME BASE_BUCKETNAME VPC_ID PUBLIC_SUBNETS [DNS_DOMAIN HOSTNAME ACM_CERT_ARN]"
    exit 1
fi

BASENAME=$1
BASE_BUCKETNAME=$2
VPC_ID=$3
PUBLIC_SUBNETS=$4

if [[ $# -gt 4 ]] ; then
    DNS_DOMAIN=$5
    HOSTNAME=$6
    ACM_CERT_ARN=$7
else
    DNS_DOMAIN=""
    HOSTNAME=""
    ACM_CERT_ARN=""
fi

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
    "ParameterKey":     "DNSDomain",
    "ParameterValue":   "${DNS_DOMAIN}"
  },
  {
    "ParameterKey":     "Hostname",
    "ParameterValue":   "${HOSTNAME}"
  },
  {
    "ParameterKey":     "ACMCertificateArn",
    "ParameterValue":   "${ACM_CERT_ARN}"
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

STACK_ID=$(aws cloudformation create-stack \
               --stack-name ${BASENAME} \
               --template-body file://scripts/cloudformation.yml \
               --capabilities CAPABILITY_NAMED_IAM \
               --parameters "$(< /tmp/cfparams.json)" \
               --output text --query 'StackId')

echo "waiting on stack: ${STACK_ID}"

aws cloudformation wait stack-create-complete --stack-name ${STACK_ID}
