LambdaPhoto - A Java Web-app Running on AWS Lambda

# Components

# Web-App Implementation

There is a single web-app dispatcher, that takes and returns a map (transformed to/from JSON by Lambda).

The dispatcher calls an appropriate service method, based on the action (extracted from the original request by API Gateway) and HTTP method.

The dispatcher knows which methods require authorization, and will check prior to calling the method (or return a "not authorized" response).


## Request Map

| Field                 | Description |
|-----------------------|-------------|
| `action`              | The request action, extracted from the URL by API Gateway. Used to dispatch the request. |
| `httpMethod`          | The HTTP request method; currently we only use GET and POST. |
| `accessToken`         | (optional) A Cognito IDP access token, used to authorize the request. Extracted from cookies by API Gateway. |
| `refreshToken`        | (optional) A Cognito IDP refresh token, used to regenerate a missing/expired access token. Extracted from cookies by API Gateway. |
| `body`                | Request-specific map of request data. |


## Response Map

| Field                 | Description |
|-----------------------|-------------|
| `responseCode`        | A string that describes the result of the operation. The value "SUCCESS" indicates success everywhere; other values are service-specific. |
| `description`         | Text to display to the user in the event of a non-success response. |
| `accessTokenHeader`   | (optional) A Cognito IDP access token, used to authorize the request. If present, will be provided in a Set-Cookie header by API Gateway. |
| `refreshTokenHeader`  | (optional) A Cognito IDP refresh token, used to regenerate a missing/expired access token. If present, will be provided in a Set-Cookie header by API Gateway. |
| `data`                | Action-specific data object. This may be a map, an array, or nil. |


## Authorization


# Client Implementation

# Building and Running

## Building the software

## Deploying to AWS

Deploying the application is a multi-step process. Parts are handled by CloudFormation, and parts are handled by shell scripts.
Run all scripts from the base directory

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
    Scripts/create_pool.sh POOL_NAME
    ```

    This script will create the pool and also a single client application with the same name. The pool will apply
    some basic password rules: a mix of uppercase, lowercase, and numbers, with at least 8 characters.
    
    When you run the script it will output the pool ID and client ID; you'll need these for the final step.
    
3. Create the database and plumbing

    ```
    insert script here
    ```
    
    This script runs a CloudFormation template to create the "unchanging" infrastructure (versus the API Gateway
    and Lambda procs, which you might wish to change). It will output some IDs that you need for the next step.
    
4. Create the API Gateway and Lambda procs.

    ```
    create_lambda.sh BASE_NAME BUCKET_NAME POOL_ID CLIENT_ID
    ```


## Invoking via CURL

    curl -v -b ~/tmp/cookies.dat -H 'Content-Type: application/json' -d '{"argle": "bargle", "fribble": 123}' -H "X-Example: argle" -H "X-Example: bargle" https://ttnpr159sc.execute-api.us-east-1.amazonaws.com/test/api/signin
