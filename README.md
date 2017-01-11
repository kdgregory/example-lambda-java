# LambdaPhoto - A Java Web-app Running on AWS Lambda

## Components

## Web-App Implementation

The web-app is implemented as a single Lambda function, that expects and returns JSON formated per the API Gateway
Proxy [docs](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-set-up-simple-proxy.html).

The last component of the request URL is the action to be performed. API Gateway extracts this as a path parameter
named `action`. 

A normal response (HTTP status code 200) contains a JSON body with the following fields:

| Field                 | Description |
|-----------------------|-------------|
| `responseCode`        | A string that describes the result of the operation. The value "SUCCESS" indicates success everywhere; other values are service-specific. |
| `description`         | Text to display to the user in the event of a non-success response. |
| `data`                | Action-specific data object. This may be a any JSON-serializable type, but will generally be a map or list. Operations that do not return data may omit this field entirely. |

Error responses omit the body, and use one of the following status codes: 

* 400 to indicate any error with the request content (generally, an untranslateable body).
  Translateable-but-incorrect body content will typically be reported using a non-success
  response code.
* 404 to indicate an invalid endpoint.
* 500 to indicate an unexpected server error.

Credentials are managed as tokens (access and refresh), and transmitted to/from the server via http-only cookies. See the Authentication section for more information.


## Client Implementation

## Authentication


## Building and Running

### Building the software

The Java components are built using Maven, with all dependencies available from Maven Central.

** Not strictly true: right now I'm depending on a snapshot revision of the kdgcommons library; 
you'll have to [check it out](https://sourceforge.net/p/kdgcommons/code/HEAD/tree/) and build yourself **

    ( cd Webapp ; mvn clean install )

### Deploying to AWS

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

    ** this step isn't currently supported **

    ```
    Scripts/create_pool.sh POOL_NAME
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
    create_lambda.sh BASE_NAME BUCKET_NAME POOL_ID CLIENT_ID
    ```


### Invoking via CURL

    curl -v -b ~/tmp/cookies.dat -H 'Content-Type: application/json' -d '{"email": "example@mailinator.com", "password": "MyCoolPassword123"}' https://7nb67d5al6.execute-api.us-east-1.amazonaws.com/test/api/signin
