#!/bin/bash
################################################################################################
#
# Creates the S3 bucket used by the app, and deploys the static content and JARs. If the
# bucket already exists, will redeploy the content and JARs.
#
#   create_bucket.sh BUCKET_NAME
#
# You must run this script from the root directory of the project. Prior to running this
# script you must have run "mvn install" in the Webapp and Resizer folders, so that the
# JARs are in your local repository. You must also have "jq" installed.
#
################################################################################################

set -e

. Scripts/_common.sh

BUCKET_NAME=$1


##
## Bucket creation will succeed even if the bucket already exists (this is alluded-to in the
## API docs, not mentioned at all in the CLI docs, so may change in the future)
##

aws s3 mb s3://${BUCKET_NAME}

##
## If the bucket already exists, we want to clear the way for our new content
##

aws s3 rm s3://${BUCKET_NAME}/${DEPLOYMENT_PREFIX} --recursive
aws s3 rm s3://${BUCKET_NAME}/${STATIC_PREFIX} --recursive

##
## Now we can populate
##

aws s3 cp ${WEBAPP_SOURCE} s3://${BUCKET_NAME}/${DEPLOYMENT_PREFIX}/${WEBAPP_FILE}
aws s3 cp ${RESIZER_SOURCE} s3://${BUCKET_NAME}/${DEPLOYMENT_PREFIX}/${RESIZER_FILE}

pushd Static
aws s3 cp . s3://${BUCKET_NAME}/${STATIC_PREFIX} --recursive
popd
