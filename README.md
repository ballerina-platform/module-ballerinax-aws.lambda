# Ballerina AWS Lambda Extension

Annotation based AWS Lambda extension implementation for Ballerina. 

[![Daily build](https://github.com/ballerina-platform/module-ballerinax-aws.lambda/workflows/Daily%20build/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-aws.lambda/actions?query=workflow%3A%22Daily+build%22)
[![Build master branch](https://github.com/ballerina-platform/module-ballerinax-aws.lambda/workflows/Build%20master%20branch/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-aws.lambda/actions?query=workflow%3A%22Build+master+branch%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-aws.lambda/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinax-aws.lambda)

## Supported Annotations:

### @lambda:Function
- Supported with Ballerina package level functions

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

Generating executables
	functions.jar
	@aws.lambda:Function: echo, uuid, ctxinfo, notifySQS, notifyS3, notifyDynamoDB, notifySES, notifyEventBridge, notifySNS, notifyKinesis, apigwRequest, urlRequest, urlCustomResponse

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.<FUNCTION_NAME> --runtime provided.al2023 --role <LAMBDA_ROLE_ARN> --layers arn:aws:lambda:<REGION_ID>:367134611783:layer:ballerina-jre21:1

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip
```

### Migrating a function created on the retired `provided` runtime

AWS stopped accepting the `provided` runtime for new functions in February 2024, so a function
created before then is still configured with a runtime that can no longer be selected. Move it to
`provided.al2023` once, and then redeploy as usual:

```bash
aws lambda update-function-configuration --function-name <FUNCTION_NAME> --runtime provided.al2023
```

## Container Image Deployment

By default `bal build` produces a ZIP deployment package, which AWS Lambda limits to 250 MB
unzipped. Building with `--cloud=aws_lambda_image` instead packages the function as a container
image, which supports images up to 10 GB:

```bash
$ bal build --cloud=aws_lambda_image
Compiling source
	functions.bal

Generating executables
	@aws.lambda:Building the container image using Docker. This may take a while.

	@aws.lambda:Function: echo, uuid, notifySQS
	Built the container image functions:0.1.0.

	Run the following commands to push the image to Amazon ECR:
	aws ecr create-repository --repository-name functions --region <REGION_ID>
	aws ecr get-login-password --region <REGION_ID> | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.<REGION_ID>.amazonaws.com
	docker tag functions:0.1.0 <ACCOUNT_ID>.dkr.ecr.<REGION_ID>.amazonaws.com/functions:0.1.0
	docker push <ACCOUNT_ID>.dkr.ecr.<REGION_ID>.amazonaws.com/functions:0.1.0

	Run the following command to deploy each Ballerina AWS Lambda function...
	aws lambda create-function --function-name <FUNCTION_NAME> --package-type Image --code ImageUri=<ACCOUNT_ID>.dkr.ecr.<REGION_ID>.amazonaws.com/functions:0.1.0 --role <LAMBDA_ROLE_ARN> --image-config '{"Command":["functions.<FUNCTION_NAME>"]}'
```

The image is named after the package and tagged with the package version. Docker must be
available on the build machine.

The handler is not baked into the image. It is selected per function with `--image-config`, so a
single image can serve every `@lambda:Function` in the package, matching how one ZIP serves every
function today.

Combining `--cloud=aws_lambda_image` with `--graalvm` produces a native image on the
`provided.al2023` base image instead of a JVM image on `public.ecr.aws/lambda/java:21`, giving a
considerably smaller image and faster cold starts.
