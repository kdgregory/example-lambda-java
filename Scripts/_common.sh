################################################################################################
#
# Provides common function and variable definitions for other scripts
#
################################################################################################

# we need this for constructing policies, and there doesn't seem to be a better way to get it

AWS_ACCOUNT_ID=$(aws iam get-user | jq -r ".User.Arn" | sed -e 's/arn.*:://' | sed -e 's/:.*//')


# this temporary directory *won't* get deleted, so we can review what's in it

TMPDIR=/tmp/example-java-lambda

if [ ! -d "${TMPDIR}" ]; then
    mkdir "${TMPDIR}"
fi


# Prefixes within the app bucket

DEPLOYMENT_PREFIX="deployment"
STATIC_PREFIX="static"


# JARfile containing the webapp

WEBAPP_SOURCE=${HOME}/.m2/repository/com/kdgregory/example/lambda/webapp/1.0-SNAPSHOT/webapp-1.0-SNAPSHOT.jar
WEBAPP_FILE=$(basename "${WEBAPP_SOURCE}")


# JARfile containing the resizer

RESIZER_SOURCE=${HOME}/.m2/repository/com/kdgregory/example/lambda/resizer/1.0-SNAPSHOT/resizer-1.0-SNAPSHOT.jar
RESIZER_FILE=$(basename "${RESIZER_SOURCE}")
