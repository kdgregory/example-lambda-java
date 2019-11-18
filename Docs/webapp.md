# Web-App Implementation

The web-app is implemented as a single Lambda function, that expects to be invoked from an
[Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html).

## Request/Response Formats

Requests use a URL with the format `/api/ACTION`. They do not include additional components or
query string parameters. All requests that provide data use a POST, with request-specific data
as a JSON object in the request body.

All response bodies have the same format: a wrapper with the following fields.

| Field                 | Description |
|-----------------------|-------------|
| `responseCode`        | A string that describes the result of the operation. The value "SUCCESS" indicates success everywhere; other values are service-specific.
| `description`         | Text to display to the user in the event of a non-success response.
| `data`                | Service-specific data object. May be any serializable object (most often a Map or List), or may be omitted.

In addition to the `responseCode`, clients must also check the HTTP status code for non-service errors:

* 200 is success; look at the response body for more information.
* 400 indicates any error with the request content (generally, an untranslateable body).
  Translateable-but-incorrect body content is usually reported via `responseCode`.
* 404 to indicate an invalid endpoint.
* 500 to indicate an unexpected server error. The cause will be logged but not returned to the caller.


## Dispatcher

At the time I wrote this example, the predominant way to write a Java web-app was the servlet API.
Lambda functions, however, handled web requests using their own "event" format. While there is an
[AWSLabs project]( https://github.com/awslabs/aws-serverless-java-container) to wrap Lambda
request events with a servlet-compliant interface, it was developed at about the same time as
this example and so I went down my own path.

I decided to implement routing similar to that used by Node's [Express](https://expressjs.com/)
framework: each action is associated with a handler function, and there's a simple `switch` that
decides which function to invoke:

```
switch (request.getAction())
{
    case RequestActions.SIGNIN :
        return invokeIf(request, HttpMethod.POST, r -> userService.signIn(r));
    // ...
    case RequestActions.UPLOAD :
        return invokeIf(request, HttpMethod.POST, authorized(r -> photoService.upload(r)));
    default:
        return new Response(404);
}
```

The `invokeIf()` method verifies that the request method matches the required method (GET or POST),
and invokes a Java function if that's the case. This technique allows functional composition, which
is how I managed authorization: `authorized()` takes a function and returns a new function that
verifies the user is authorized before invoking the original function.


## Services

At present there are only two services in the web-app:

* `UserService`, which manages authentication and authorization.
* `PhotoService`, which handles uploads and list operations.

A singleton instance of each service is instantiated by the `Dispatcher` when it's constructed (ie,
at the time of first invocation). These services in turn instantiate whatever objects they need,
such as AWS service clients.

At this point it's a good idea to review the rules of instance variables in Lambda:

* Lambda functions are permitted to use instance variables, and _should_ use instance variables for
  long-lived or expensive-to-produce objects.
* Any given Lambda invocation may or may not re-use an existing container. As a result, it has to
  be prepared to initialize or re-initialize variables as needed (here, the constructor does this).
* Instance variables must _not_ be used to retain client state across invocations, because there's
  requests from the same client may go to different containers.
* Anything assigned to an instance variable must not rely on its finalizer being run; it should
  assume that the container shuts down with a hard kill.
* While the available documentation strongly hints that each concurrent Lambda function will be
  invoked in a separate container, _at no point does it clearly state this_. The best you get 
  is in the [FAQ](https://aws.amazon.com/lambda/faqs/), which states that "Each AWS Lambda function
  runs in its own isolated environment" but then goes on to compare those environments to EC2.
  As a result, I recommend that you _maintain normal thread-safety practices_ for the instance
  variables of Lambda functions.


### Authentication / Authorization

User management and authentication is handled by [Cognito](http://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools.html).
Any user is allowed to sign up for the service, by providing a valid email address. For more
information on Cognito, see my [blog post](http://blog.kdgregory.com/2016/12/server-side-authentication-with-amazon.html).

On successful signin, Cognito returns an access token and a refresh token. The access token
is a [JSON Web Token](https://jwt.io/) that contains information about the user and expires
after one hour (this time limit is set by Cognito). The refresh token is an opaque token
that can be presented to Cognito to get a new access token; its expiration is controlled by
the pool, and my default configuration is 7 days.

> JSON Web Tokens have received a lot of unfavorable comment, largely as a result of poor
  implementation. I believe that as long as you (1) use RSA-based signatures and keep the
  secret key secret (which Amazon does), and (2) use a library that checks the algorithm
  and not just the signature (which I believe jose4j does), then you'll be OK.

The tokens are passed to the client using HTTP-only cookies (so they're not accessible to
code running on the page). Cookie management is a minor headache with Lambda: you have to
parse and produce a single `Cookies` header, and there isn't (as-of my implementation) a
library to do this for you. Take a look at the [Tokens](../Webapp-Lambda/src/main/java/com/kdgregory/example/javalambda/webapp/util/Tokens.java)
class to see how I did it.

Beware that the application does _not_ attempt to prevent cross-site request forgery
([CSRF](https://en.wikipedia.org/wiki/Cross-site_request_forgery)); if malicious code
can get access to the user's access and refresh tokens, it can access the user's photos.
Adding such protection would require changing the services to produce and expect an
additional token, delivered in the request body.
