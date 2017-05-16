# LambdaPhoto - A Java Web-app Running on AWS Lambda

## System Architecture

**TBD**


## Static Content

Static content is stored on S3, using the same bucket as photo storage and deployment bundles. It is (currently)
delivered using API Gateway.


## Web-App Implementation

The web-app is implemented as a single Lambda function, that expects and returns JSON formatted per the API Gateway
Proxy [docs](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-set-up-simple-proxy.html).

The `Dispatcher` extracts information from the standard request JSON, creating a `Request` object. It then invokes
an appropriate service method, based on the last component of the URL (which API Gateway extracts into the `action`
path parameter). The service method creates a `Response` object, which the dispatcher packages into the API Gateway
format.

The dispatcher will return 200 if it can process the request, whether or not the outcome is successful from the
client perspective. It returns the following status codes (and no response body) if unable to process the request:

* 400 to indicate any error with the request content (generally, an untranslateable body).
  Translateable-but-incorrect body content will typically be reported using a non-success
  response code.
* 404 to indicate an invalid endpoint.
* 500 to indicate an unexpected server error. The cause will be logged.

Request bodies are defined by the service being called (and may not exist). Response bodies have the following fields:

| Field                 | Description |
|-----------------------|-------------|
| `responseCode`        | A string that describes the result of the operation. The value "SUCCESS" indicates success everywhere; other values are service-specific. |
| `description`         | Text to display to the user in the event of a non-success response. |
| `data`                | Action-specific data object. This may be a any JSON-serializable type, but will generally be a map or list. Operations that do not return data may omit this field entirely. |


### Authentication

User management and authentication is handled by Cognito IDP. Any user is allowed to sign up for the service, by
providing a valid email address (the service sends a temporary password to that address).

After successiful sign-in, the service provides access and refresh tokens, which are managed as HTTP-only cookies.
Any response may include `Set-Cookie` headers, as a result of token refresh.

**TBD**: describe handling of authenticated requests.


## Database

All file metadata is stored in a DynamoDB table that has the following attributes:

| Attribute Name    | Description
|-------------------|------------
| `id`              | Unique identifier for the file, assigned during upload.
| `user`            | User that owns the file. Also assigned during upload, based on logged-in user.
| `filename`        | The original filename.
| `description`     | User-provided description.
| `mimetype`        | The standard MIME type for the file.
| `uploadedAt`      | The millis-since-epoch timestamp when the file was uploaded.
| `sizes`           | An array(string) that identifies the various resolutions that have been saved for the file. Includes `fullsize` and a collection of `WxH` smaller sizes.


## Client Implementation

The client is a single-page application using AngularJS 1.x.


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

To run this script you also need to define environment variables with your access key and
region:

* `AWS_ACCESS_KEY`
* `AWS_SECRET_KEY`
* `AWS_REGION`

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

    curl -v -c /tmp/cookies.dat -H 'Content-Type: application/json' -d '{"email": "example@mailinator.com", "password": "MyCoolPassword123"}' https://ENDPOINT.execute-api.us-east-1.amazonaws.com/test/api/signin

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
