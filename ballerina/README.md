## Overview

The AWS Lambda connector provides the capabilities of creating [AWS Lambda](https://aws.amazon.com/lambda/) functions.

### Key Features

- Create and deploy serverless functions on AWS Lambda
- Handle events from multiple AWS services including SQS, S3, DynamoDB, SES, and API Gateway
- Access request execution context information within Lambda functions

### Annotation Usage Sample:

```ballerina
import ballerinax/aws.lambda;
import ballerina/uuid;
import ballerina/io;

// The `@lambda:Function` annotation marks a function to
// generate an AWS Lambda function
@lambda:Function
public function echo(lambda:Context ctx, json input) returns json {
   return input;
}

@lambda:Function
public function uuid(lambda:Context ctx, json input) returns json {
   return uuid:createType1AsString();
}

// The `lambda:Context` object contains request execution
// context information
@lambda:Function
public function ctxinfo(lambda:Context ctx, json input) returns json|error {
   json result = { RequestID: ctx.getRequestId(),
                   DeadlineMS: ctx.getDeadlineMs(),
                   InvokedFunctionArn: ctx.getInvokedFunctionArn(),
                   TraceID: ctx.getTraceId(),
                   RemainingExecTime: ctx.getRemainingExecutionTime() };
   return result;
}

@lambda:Function
public function notifySQS(lambda:Context ctx,
                          lambda:SQSEvent event) returns json {
    return event.Records[0].body;
}

@lambda:Function
public function notifyS3(lambda:Context ctx,
                         lambda:S3Event event) returns json {
    return event.Records[0].s3.'object.key;
}

@lambda:Function
public function notifyDynamoDB(lambda:Context ctx,
                               lambda:DynamoDBEvent event) returns json {
    return event.Records[0].dynamodb.Keys.toString();
}

@lambda:Function
public function notifySES(lambda:Context ctx,
                          lambda:SESEvent event) returns json {
    return event.Records[0].ses.mail.commonHeaders.subject;
}

@lambda:Function
public function notifyEventBridge(lambda:Context ctx,
                                  lambda:EventBridgeEvent event) returns json {
    return event.detail\-type;
}

@lambda:Function
public function notifySNS(lambda:Context ctx,
                          lambda:SNSEvent event) returns json {
    return event.Records[0].Sns.Message;
}

@lambda:Function
public function notifyKinesis(lambda:Context ctx,
                              lambda:KinesisEvent event) returns json {
    return event.Records[0].kinesis.partitionKey;
}

@lambda:Function
public function apigwRequest(lambda:Context ctx,
                             lambda:APIGatewayProxyRequest request) {
    io:println("Path: ", request.path);
}

@lambda:Function
public function urlRequest(lambda:Context ctx,
                           lambda:FunctionURLRequest request) returns json {
    return {method: request.requestContext.http.method, path: request.rawPath};
}

@lambda:Function
public function urlCustomResponse(lambda:Context ctx,
                                  lambda:FunctionURLRequest request) returns json {
    lambda:FunctionURLResponse response = {
        statusCode: 201,
        headers: {"Content-Type": "application/json"},
        body: "{\"message\":\"Hello, world!\"}"
    };
    return response.toJson();
}
```

The output of the bal build is as follows:

```bash
$ bal build functions.bal
Compiling source
    functions.bal

Generating executable
	functions.jar
	@aws.lambda:Function: echo, uuid, ctxinfo, notifySQS, notifyS3, notifyDynamoDB, notifySES, notifyEventBridge, notifySNS, notifyKinesis, apigwRequest, urlRequest, urlCustomResponse

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name $FUNCTION_NAME --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.$FUNCTION_NAME --runtime provided.al2023 --role $LAMBDA_ROLE_ARN --layers arn:aws:lambda:$REGION_ID:367134611783:layer:ballerina-jre21:1 --memory-size 512 --timeout 10

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name $FUNCTION_NAME --zip-file fileb://aws-ballerina-lambda-functions.zip
```

- For information on the operations, which you can perform with this module, see [Classes](/learn/api-docs/ballerina/index.html#/ballerinax/aws.lambda/0.0.0/aws.lambda/classes/Context).
- For information on the deployment, see the [AWS Lambda Deployment Guide](/learn/deployment/aws-lambda/).

### Report Issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina AWS Lambda repository](https://github.com/ballerina-platform/module-ballerinax-aws.lambda).

### Useful Links
- Discuss code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
- Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.

### Migrating a function created on the retired `provided` runtime

A function created before February 2024 is still configured with the `provided` runtime, which AWS
no longer accepts for new functions. Move it to `provided.al2023` once, then redeploy as usual:

```bash
aws lambda update-function-configuration --function-name $FUNCTION_NAME --runtime provided.al2023
```

## Container Image Deployment

`bal build` produces a ZIP deployment package by default, which AWS Lambda limits to 250 MB
unzipped. Build with `--cloud=aws_lambda_image` to package the function as a container image
instead, which supports images up to 10 GB:

```bash
bal build --cloud=aws_lambda_image
```

The image is named after the package and tagged with the package version, and Docker must be
available on the build machine. The handler is selected per function at deployment time with
`--image-config`, so a single image can serve every `@lambda:Function` in the package. Adding
`--graalvm` produces a smaller native image on the `provided.al2023` base image.

## Lambda Destinations

A function can route the result of an **asynchronous** invocation to another AWS service, by
declaring destinations on the annotation:

```ballerina
@lambda:Function {
    destinations: {
        onSuccess: "arn:aws:sqs:<REGION_ID>:<ACCOUNT_ID>:orders-processed",
        onFailure: "arn:aws:sns:<REGION_ID>:<ACCOUNT_ID>:alerts"
    }
}
public function processOrder(lambda:Context ctx, lambda:SQSEvent event) returns json {
    return event.Records[0].body;
}
```

Either field may be omitted, and a destination may be an SQS queue, an SNS topic, an EventBridge
event bus or another Lambda function. `bal build` then prints the command that configures them:

```bash
aws lambda put-function-event-invoke-config --function-name processOrder --destination-config '{"OnSuccess":{"Destination":"arn:aws:sqs:..."},"OnFailure":{"Destination":"arn:aws:sns:..."}}'
```

Two things to know:

- Destinations apply to **asynchronous invocations only**. A function invoked synchronously, such as
  through a function URL, returns its result to the caller and never routes to a destination.
- The function's execution role needs permission to write to the destination, for example
  `sqs:SendMessage` for an SQS queue. Without it the invocation succeeds but the record is never
  delivered, which Lambda reports through the `DestinationDeliveryFailures` CloudWatch metric rather
  than as an invocation error, so that metric is worth alerting on.

`@lambda:Function` remains valid with no annotation value, so existing functions need no change.
