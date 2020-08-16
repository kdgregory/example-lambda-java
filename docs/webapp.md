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

* `AuthService`, which verifies that the request has a valid access token, and retrieves the
  user's information from Cognito.
* `PhotoService`, which handles uploads and list operations.

An instance of each service is instantiated by the `Dispatcher` when it's constructed (ie, at the
time of first invocation). These services in turn instantiate whatever objects they need, such as
AWS service clients.

At this point it's a good idea to review the rules of instance variables in Lambda:

* Lambda functions are permitted to use instance variables, and _should_ use instance variables
  for long-lived or expensive-to-produce objects.
* Any given Lambda invocation may or may not re-use an existing container. As a result, it has to
  be prepared to initialize or re-initialize variables as needed (in this app, when the main-class
  constructor runs).
* Instance variables must _not_ be used to retain client state across invocations, because there's
  no guarantee that the same container will be reused for a client.
* Anything assigned to an instance variable must not rely on its finalizer being run; it should
  assume that the container shuts down with a hard kill.
* Each Lambda execution environment handles one request at a time. There's no need to synchronize
  variable access unless you create your own threads (and that's generally a 
  [bad idea](https://blog.kdgregory.com/2019/01/multi-threaded-programming-with-aws.html)).
