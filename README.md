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


## Client Implementation

The client is a single-page application using AngularJS 1.x.


## Building and Running

### Building the software

The Java components are built using Maven, with all dependencies available from Maven Central.

    ( cd Webapp ; mvn clean install )

### Deploying to AWS

Deploying the application is a multi-step process. Parts are handled by CloudFormation, and parts are handled by shell scripts.
Run all scripts from the project root directory

1. Create the bucket and deploy the static files and web-app.
    ```
    Scripts/create_bucket.sh BUCKET_NAME
    ```

    Invoke with the name of the bucket that you want to use. If the bucket already exists, the existing
    files in the deployment and static directories will be removed before the new files are uploaded.

    This script will look in your local Maven repository to find the deployment JARs for Lambda, so you
    must have built those JARs before running it.

2. Create the Cognito user pool.

    ```
    Scripts/create_cognito.sh POOL_NAME
    ```

    This script will create the pool and also a single client application with the same name. The pool will apply
    some basic password rules: a mix of uppercase, lowercase, and numbers, with at least 8 characters.

    When you run the script it will output the pool ID and client ID; you'll need these for the final step.

3. Create the database and plumbing

    ** this step isn't currently supported **

    ```
    insert script here
    ```

    This script runs a CloudFormation template to create the "unchanging" infrastructure (versus the API Gateway
    and Lambda procs, which you might wish to change). It will output some IDs that you need for the next step.

4. Create the API Gateway and Lambda procs.

    ```
    Scripts/create_lambda.sh BASE_NAME BUCKET_NAME POOL_ID CLIENT_ID
    ```


### Invoking via CURL

    curl -v -c /tmp/cookies.dat -H 'Content-Type: application/json' -d '{"email": "example@mailinator.com", "password": "MyCoolPassword123"}' https://7nb67d5al6.execute-api.us-east-1.amazonaws.com/test/api/signin
