#!/bin/bash
################################################################################################
#
# Terminates the demo app, deleting all resources that were created by deploy.sh
#
#   scripts/shutdown.sh STACK_ID
#
################################################################################################

STACK_ID=$1

# grab this before we terminate the stack; can get it afterward only if using actual ID
BUCKETS=$(aws cloudformation describe-stacks --stack-name $STACK_ID \
          --output table --query 'Stacks[].Parameters[].[ParameterKey,ParameterValue]' \
          | grep BucketName | sed -e 's/.* |  *//' | sed -e 's/ .*//')

aws cloudformation delete-stack --stack-name $STACK_ID

for b in $BUCKETS ; do
    aws s3 rb --force s3://$b
    done
