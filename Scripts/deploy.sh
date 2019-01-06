#!/bin/bash
################################################################################################
#
# Deploys the demo app
#
#   Scripts/deploy.sh BASENAME BUCKETNAME
#
# Will create the bucket if it does not already exist, copies all deployment artifacts into the
# bucket, configures the swagger template with account-specific details, and creates the
# CloudFormation stack.
#
# To run, you must have standard shell programs. You must also have your environment configured
# for AWS, including AWS_REGION defined. And lastly, you must run from within the project root
# directory.
#
################################################################################################

BASENAME=$1
BUCKETNAME=$2

AWS_ACCOUNT_ID=$(aws sts get-caller-identity | grep "Account" | sed -e 's/"//g' | sed -e 's/, *$//' | sed -e 's/.* //')

# Prefixes within the app bucket

DEPLOYMENT_PREFIX="deployment"
STATIC_PREFIX="static"


# JARfile containing the webapp

WEBAPP_PATH=Webapp-Lambda/target/webapp-lambda-*.jar
WEBAPP_FILE=$(basename "${WEBAPP_PATH}")

# JARfile containing the resizer

RESIZER_PATH=Resizer-Lambda/target/resizer-lambda-*.jar
RESIZER_FILE=$(basename "${RESIZER_PATH}")

# the generated Swagger spec

SWAGGER_FILE=swagger.json
SWAGGER_PATH=Scripts/${SWAGGER_FILE}


##
## Step 1: convert the swagger template into an actual spec
##

cat Scripts/swagger.template | sed -e "s/SUBSTITUTE_BASENAME/${BASENAME}/g" \
                             | sed -e "s/SUBSTITUTE_ACCOUNT_ID/${AWS_ACCOUNT_ID}/g" \
                             | sed -e "s/SUBSTITUTE_REGION/${AWS_REGION}/g" \
                             | sed -e "s/SUBSTITUTE_BUCKET_NAME/${BUCKETNAME}/g" \
                             > ${SWAGGER_PATH}


##
## Step 2: create the bucket; this will succeed even if the bucket exists, 
##         so we need to follow it by deleting anything that's already there
##

aws s3 mb s3://${BUCKETNAME}

aws s3 rm s3://${BUCKETNAME}/${DEPLOYMENT_PREFIX} --recursive
aws s3 rm s3://${BUCKETNAME}/${STATIC_PREFIX} --recursive


##
## Step 3: upload everything to the bucket
##

aws s3 cp ${SWAGGER_PATH} s3://${BUCKETNAME}/${DEPLOYMENT_PREFIX}/${SWAGGER_FILE}
aws s3 cp ${WEBAPP_PATH} s3://${BUCKETNAME}/${DEPLOYMENT_PREFIX}/${WEBAPP_FILE}
aws s3 cp ${RESIZER_PATH} s3://${BUCKETNAME}/${DEPLOYMENT_PREFIX}/${RESIZER_FILE}

pushd Webapp-Static
aws s3 cp . s3://${BUCKETNAME}/${STATIC_PREFIX} --recursive
popd


##
## Step 4: create the CloudFormation stack
##


cat > Scripts/cfparams.json <<EOF
[
  {
    "ParameterKey":     "BaseName",
    "ParameterValue":   "${BASENAME}"
  },
  {
    "ParameterKey":     "Bucket",
    "ParameterValue":   "${BUCKETNAME}"
  },
  {
    "ParameterKey":     "WebappJar",
    "ParameterValue":   "${DEPLOYMENT_PREFIX}/${WEBAPP_FILE}"
  },
  {
    "ParameterKey":     "ResizerJar",
    "ParameterValue":   "${DEPLOYMENT_PREFIX}/${RESIZER_FILE}"
  },
  {
    "ParameterKey":     "SwaggerSpec",
    "ParameterValue":   "${DEPLOYMENT_PREFIX}/${SWAGGER_FILE}"
  }
]
EOF

aws cloudformation create-stack \
                   --stack-name ${BASENAME} \
                   --template-body file://Scripts/deploy.cf \
                   --capabilities CAPABILITY_NAMED_IAM \
                   --parameters "$(< Scripts/cfparams.json)"
