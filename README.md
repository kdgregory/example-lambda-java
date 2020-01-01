# LambdaPhoto 

This project is a simple photo-sharing site with the following features:

* Self-signup for users
* Each user can upload as many photos as they want
* Each photo will be resized to some standard "web" sizes.
* Users can share or embed photos via link

I originally wrote it early in 2017 to support a [talk](Docs/jug_presentation.pdf)
for the [Philadelphia Java Users Group](https://www.meetup.com/PhillyJUG/). Recently
(2019) I revisited it to generally clean up the code and use what I believe are
current best-practices (CloudFront/Application Load Balancer rather than API Gateway),
and perhaps use it as the base for a talk on observability in distributed systems. It
also serves as a repository of working CloudFormation snippets (something that I've
found increasingly valuable), and as a place where I can try out new web-app-related
services.

At this point I should interject that I don't think Lambda is a good implementation
plaform for a web-app: in the best case (Python or JavaScript, running outside VPC)
it adds a few dozen milliseconds to each request. And Java, with its long cold-start
times, is a particularly bad choice: in my experience the time to first response is
around 3-4 seconds (which would cause most users to hit refresh ... queueing up a new
Lambda invocation with its own cold-start). Perhaps [GraalVM](https://www.graalvm.org/)
will make this use-case work, but until then I'm sticking with EC2/ECS as the deployment
platform for Java web-apps.


## Architecture 

![Architecture Diagram](Docs/architecture.png)

The front-end is a fairly standard AWS web-app: CloudFront in front, serving (and caching)
static content from S3 and dynamic content from an Application Load Balancer (ALB). The ALB
is, by default, configured to preserve access logs in S3; this is in keeping with making an
observable application.

There are two Lambda functions: the Web-App proper and the Resizer:

* The Web-App, as its name implies, handles all of the web-app's API requests. There's
  more information about its implementation [here](Docs/webapp.md).
* The Resizer is invoked for new file upload, at the current time using an SNS topic as
  trigger. It transforms the uploaded image into a set number of standard sizes. The
  choice of topic versus SQS queue was driven by Lambda not supporting SQS triggers at
  the time this example was written.

> Note: files are uploaded via the API. This dramatically limits file size: [the request
  body is limited to 1MB](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html)
  for Lambda invocations, and that's after Base64 encoding. The next revision will
  enable direct client upload, removing this limit.

Authentication is handled via a Cognito user pool. All interaction with this user pool
happens via the web-app, using the "admin" API. A future revision will move interaction
into the ALB.

User and photo metadata is stored in a DynamoDB table, keyed by `(userId, photoId)`. This
design is sub-optimal for the case where there are a small number of users, because the
database as a whole will be confined to just a few shards (perhaps one), and a user with
a large number of photos may overwhelm the throughput of that shard. This becomes less of
a problem as the userbase grows, because more shards will be added to the database. Still,
to avoid problems with users that have an overwhelming number of photos, I would consider
switching to a two-table design, where one table holds a list of photo IDs keyed by user ID
and the other contains the photo metadata, keyed by photo ID.

> One thing I would _not_ do is switch to a relational database. I originally designed this
  project before Lambda supported running in a VPC (which would mean leaving a traditional
  database server exposed to the Internet), and even after in-VPC Lambdas were a thing, the
  cold-start times would have been a non-starter. However, the main reason that I went with
  Dynamo is that it is a perfect match for the use-case: key-based retrieval of metadata
  that can be distributed across an unbounded number of nodes.


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

Deploying the application is a multi-step process. It largely depends on CloudFormation, but
there are a few components that have to be customized or created in advance. Fortunately,
it's all wrapped up in a shell script:

```
Scripts/deploy.sh BASE_NAME BASE_BUCKET_NAME VPC_ID SUBNET_IDS
```

where:

* `BASE_NAME` is a unique name that's used for the stack and all deployed components; I use `LambdaPhoto`.
* `BASE_BUCKET_NAME` is used as the prefix for all buckets used by the application (there are
   four: one each for images, static content, Lambda deployment, and ALB access logs). This
   name must be unique in all of AWS; I recommend an inverse-hostname such as `com-mycompany-lambdaphoto`.
*  `VPC_ID` and `SUBNET_IDS` are used to deploy the load balancer; the latter is a comma-separated
   list of public subnets within the VPC (eg: `subnet-123456,subnet-789012`).

To run this script you will need to have your default IAM profile configured, either via
`aws configure` or environment variables.

When you run the script, it first builds the project, then creates the buckets and copies the
deployment bundles and static content into them; you'll see various messages as this happens.
The last thing that it does is start building the CloudFormation stack; you'll see a message
like the following:

```
{
    "StackId": "arn:aws:cloudformation:us-east-1:999999999999:stack/Example/a0ccd440-3a37-11e7-89ed-50d5ca6e60e6"
}
```

It takes around 20 minutes to build the stack, largely due to the CloudFront distribution.


## Shutting Down

**Warning:** a running ALB will cost slighly under $1 per day, and you'll be charged that for
every day that you leave this stack running. To avoid this cost, you can run the shutdown script:

```
Scripts/undeploy.sh STACK_ID
```

where:

* `STACK_ID` is the ID printed when starting the stack (you can also use the stack name).

This script will delete the stack and also all buckets that were created by the `deploy` script.
If you _don't_ want to delete the buckets, delete the stack manually using the AWS Console.


## Useful cURL Commands

When you're changing the web-app service, it is easier if you separate changes to JavaScript
from changes to the Lambda, then use `curl` to invoke the operation. Making this a bit more
challenging is that you need a valid JWT token to be able to do anything. In the following
examples, replace `ENDPOINT` with the address of your load balancer or CloudFront distribution.

* Login

  Additionally replace `USERNAME` and `PASSWORD` with valid entries. Note that the JWT tokens
  are written to a file that will be readable by anyone. If you're concerned about that, use
  a directory that doesn't permit world read or set your `umask` appropriately.

  ```
  curl -v 'https://ENDPOINT/api/signin' -c /tmp/cookies.dat -H 'Content-Type: application/json' --data '{"email":"USERNAME","password":"PASSWORD"}'
  ```

  The response should be `{"data":null,"description":"","responseCode":"SUCCESS"}`.

  Note that it will take several seconds to start a new Lambda execution environment.

* Request a file upload

  Additionally replace `USERNAME`, `FILENAME, and `DESCRIPTION` with appropriate values.

  ```
  curl -v 'https://ENDPOINT/api/requestUpload' -b /tmp/cookies.dat -c /tmp/cookies.dat -H 'Content-Type: application/json' --data '{"username":"USERNAME","filename":"FILENAME","description":"DESCRIPTION","mimetype":"image/jpeg"}' > /tmp/$$
  ```

  The response will contain the signed URL as its `data` element. Since this URL is long,
  and would become intermingled with the debugging output if sent to the console, it's
  written to a file.

  This is the general format for endpoint requests that require authentication; it uses
  the cookie jar created in the prior step both to provide cookies and receive updated
  cookies.

  If you get an authentication error, verify that cookies are being sent: you should see
  a `Cookie:` line in the log output. If you don't, sign in again and make sure that the
  "cookie jar" (`/tmp/cookies.dat`) has been populated. If you do see a cookie, it's
  possible that you still need to sign in again; otherwise, debug from Lambda logs.

* Upload file content

  Replace `FILENAME` with your source filename, and `SIGNED_URL` with the URL from the
  previous step. Note that the URL has to be enclosed in single quotes to prevent the
  shell from intepreting the numerous `&`s that appear within it. Note also that the
  content type is limited to JPEG images.

  ```
  curl -v -XPUT --data-binary @FILENAME -H 'Content-Type: image/jpeg' 'SIGNED_URL'
  ```
