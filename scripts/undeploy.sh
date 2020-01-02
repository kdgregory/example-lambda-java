#!/bin/bash
################################################################################################
#
# Undeploys the demo app, deleting all resources that were created by deploy.sh
#
#   scripts/undeploy.sh STACK_ID
#
################################################################################################

STACK_ID=$1

BUCKETS=$(aws cloudformation describe-stacks --stack-name $STACK_ID \
          --output table --query 'Stacks[].Parameters[].[ParameterKey,ParameterValue]' \
          | grep BucketName | sed -e 's/.* |  *//' | sed -e 's/ .*//')

for b in $BUCKETS ; do
    aws s3 rb --force s3://$b
    done

# this should complete now that the buckets have been forcibly removed
aws cloudformation delete-stack --stack-name $STACK_ID
