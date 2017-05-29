# Web-App Implementation

The web-app is implemented as a single Lambda function, that expects and returns JSON formatted per the API Gateway
Proxy [docs](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-set-up-simple-proxy.html).

## Request/Response Formats

All requests and responses are JSON. Requests are defined by the service, and may not exist (in the case of a GET).

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

Because Lambda only allows a single entry point, the web-app uses the [Front Controller](https://en.wikipedia.org/wiki/Front_controller)
pattern, with the [Dispatcher](../Webapp-Lambda/src/main/java/com/kdgregory/example/javalambda/webapp/Dispatcher.java)
being the entry point.

At the time this example was written, there wasn't a standard request handler for Lambda functions. The
approach that I chose has the following steps:

* `Dispatcher.handler()` receives a proxy request from API Gateway and extracts relevant information,
  including the action to be performed, the authentication tokens, and the request body. It stores all
  of these in a `Request` object. This function is where all of the top-level exception handling lives;
  exceptions are translated into an appropriate error status.
* `Dispatcher.dispatch()` takes that request and identifies the service function that will handle it.
  See below for more information.
* The various service methods are expected to return a `Response` object or throw a catchable exception.

The `dispatch()` method is somewhat interesting because it uses actual lambdas (as in Java8 lambdas) to
dispatch the request. At the top level there's a `switch` statement that looks at the request action.
Assuming a match, the second-level check against request method is handled by the `invokeIf()` method.
This method also takes a Java8 `Function` that accepts a `Request` and returns a `Response`, and invokes
that function if the method matches.

This approach has both positives and negatives. The primary negative is that it won't easily extend to
multiple methods for the same action. That could be implemnted either with a varargs version of
`invokeIf()`, or by replacing the switch with an if-else tree.

On the other hand, invoking the service via a `Function` means that we can compose functions, and this
is how authorization works: those services that need an auth check can wrap the service invocation
with a function that performs that check. As implemented, if the auth check succeeds the `Request`
object is updated with the ID of the user making that request.


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
