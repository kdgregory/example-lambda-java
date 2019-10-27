# LambdaPhoto 

This project is a simple photo-sharing site with the following features:

* Self-signup for users
* Each user can upload as many photos as they want
* Each photo will be resized to some standard "web" sizes.
* Users can share or embed photos via link

It was originally written to support a [presentation](Docs/jug_presentation.pdf)
for the [Philadelphia Java Users Group](https://www.meetup.com/PhillyJUG/), focused
on how you would use Java with AWS Lambda.

As such, **it should not be considered production code**. In particular, don't use
API Gateway to serve static content, use CloudFront. On the other hand, I think the
Java side is a reasonable implementation.


## Architecture Diagram

![Architecture Diagram](Docs/architecture.png)


## What's in the Package

* `Docs`              - Additional implementation documentation.
* `Lib-Shared`        - Code that is shared between the WebApp and Resizer (the DynamoDB and S3 code lives here).
* `pom.xml`           - Maven build file.
* `README.md`         - This file.
* `Resizer-Lambda`    - A Lambda function that resizes photos in response to a message on SNS.
* `Scripts`           - Deployment scripts (see below).
* `Webapp-Lambda`     - A Lambda function that implements a simple WebApp.
* `Webapp-Static`     - Static content for the WebApp.


## Building and Deploying

### Building the Java components

The Java components are built using Maven, with all dependencies available from Maven Central.

    mvn clean install


### Deploying to AWS

Deploying the application is a multi-step process. It largely depends on CloudFormation, but
there are a few components that have to be customized or created in advance. Fortunately,
it's all wrapped up in a shell script:

    Scripts/deploy.sh BASE_NAME BUCKET_NAME

* `BASE_NAME` is a unique prefix for the deployed components; I use `LambdaPhoto`.
* `BUCKET_NAME` is the name of an S3 bucket that is used by the application. This name has
  to be globally unique in the S3 world; I recommend an inverse-hostname approach similar
  to Java packages (eg: `com-example-lambda` -- although that's probably already in use).

To run this script you will need to have your default IAM profile configured, either via
`aws configure` or environment variables.

Assuming that all runs, you'll see a bunch of messages as the script generates and copies
the deployment scripts, and it should end with a CloudFormation message like the following:

    {
        "StackId": "arn:aws:cloudformation:us-east-1:999999999999:stack/Example/a0ccd440-3a37-11e7-89ed-50d5ca6e60e6"
    }

At this point you've created two AWS resources, a bucket and a CloudFormation stack. You'll
have to manually delete these when you're done with the example.

The CloudFormation stack in turn creates all other components. You can monitor its progress
via the [AWS Console](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks?filter=active)
(if you're not in the `us-east-1` region you'll need to switch regions after clicking that
link). It will take several minutes to create all of the components, at which time the stack
status will change to `CREATE_COMPLETE`.

There aren't a lot of reasons for the stack creation to fail: the most common that I experienced
was because I didn't build the Java packages beforehand. It's also possible that you don't
have the permissions required to create a stack (this is likely in a corporate environment,
unlikely if it's your personal account). In any case, if you see `ROLLBACK_COMPLETE` as the
stack status you'll need to figure out what went wrong (look through the events), correct
it if you can, delete the failed stack, and retry.


### Smoketest

Curl is your new best friend:

    curl -v -c /tmp/cookies.dat -H 'Content-Type: application/json' -d '{"email": "example@mailinator.com", "password": "MyCoolPassword123"}' https://ENDPOINT/test/api/signin

You'll need to replace `ENDPOINT` with the actual DNS name of the endpoint, but the rest of the
request doesn't matter: in a new deployment you won't have any users. If everything's working,
this request should result in something like the following, and you'll see CloudWatch logs for
the Webapp lambda function.

    * Hostname was NOT found in DNS cache
    *   Trying 52.84.86.47...
    * Connected to 7nr07tcg55.execute-api.us-east-1.amazonaws.com (52.84.86.47) port 443 (#0)
    ...
    * upload completely sent off: 68 out of 68 bytes
    < HTTP/1.1 200 OK
    ...
    {"data":null,"description":"Incorrect username or password","responseCode":"INVALID_USER"}
