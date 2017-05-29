# Resizer Implementation

The Resizer exists to demonstrate asynchronous Lambda invocation; it uses the
[Java Image IO](http://docs.oracle.com/javase/8/docs/technotes/guides/imageio/index.html)
package to produce fixed-size versions of the original image.


## SNS vs SQS vs S3

SNS (the Simple Notification Service) seems to be a strange choice for communication
between the webapp and resizer: it's a pub-sub messaging service, and we only have
one consumer. You might think that SQS (the Simple Queue Service) would be a better
choice. I did as well, until I learned that SQS can't be used to invoke Lambda. I'm
not sure why this is the case; perhaps it has something to do wtih the application
being responsible for removing the message from the queue. Regardless, SQS was out.

One possible alternative would be S3 PUT notifications: these are used for many Lambda
examples (including [my own example](https://github.com/kdgregory/example-maven-s3-lambda-nexus)
that republishes Maven artifacts). The chief drawback of this approach is that we'll
get events when storing the resized photos. And while it's easy enough to ignore those
events based on the bucket key, we'd still be paying for each request.


## Invocation

To access a photo's metadata, consumers must know both the photo's owner and its GUID.
For a more complex payload, I would look to something like JSON, but in this case I
used as simple `USERNAME/GUID` message. The `PhotoKey` class in the shared library
performs the translation.

Each Amazon service has its own event format; for SNS, you can see an example event
[here](http://docs.aws.amazon.com/lambda/latest/dg/eventsources.html#eventsources-sns).
As with the web-app, I let Lambda translate the event into a hierarchical `Map`.

The [documentation](http://docs.aws.amazon.com/lambda/latest/dg/concurrent-executions.html)
indicates that each separate event is a unit of work, and will result in a concurrent
Lambda execution. However, note that the event itself contains an array of records. That
may be an artifact of a previous multi-event architecture, or it may be a hint of things
to come; I strongly recommend processing all records in a loop (as I've done) rather than
assuming there will only be one.

You might be wondering what happens if your lambda function throws an exception. The
answer is that it's [retried twice](http://docs.aws.amazon.com/lambda/latest/dg/retries-on-errors.html),
after which the message is sent to a dead-letter queue -- **if one exists**. I haven't
defined such a queue for this example, which means that the event will be discarded
after those retries. Since you can't process such a queue using Lambda, it serves
primarily for forensic examination, and I've already got logs for that. If you can't
afford to lose messages, be sure to define the DLQ.


## Permissions

When you create a Lambda function using the AWS Console, you attach an event trigger
to that function and It Just Works. However, if you're using
[CloudFormation](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html)
to configure your Lambda functions, it's not immediately apparent how to do this: the
`EventSourceMapping` template is only for streaming sources, and there isn't a separate
`Trigger` template.

The answer is that you have to configure the SNS topic: you add a subscription to the topic and
you must also grant the `lambda:InvokeFunction` permission to the topic. You'll see this toward
the end of my [example template](../Scripts/deploy.cf).
