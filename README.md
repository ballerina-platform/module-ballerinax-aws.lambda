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
public function apigwRequest(lambda:Context ctx, 
                             lambda:APIGatewayProxyRequest request) {
    io:println("Path: ", request.path);
}
```

The output of the bal build is as follows:

```bash
$ bal build functions.bal 
Compiling source
	functions.bal

Generating executables
	functions.jar
	@aws.lambda:Function: echo, uuid, ctxinfo, notifySQS, notifyS3, notifyDynamoDB, notifySES, apigwRequest

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.<FUNCTION_NAME> --runtime provided --role <LAMBDA_ROLE_ARN> --layers arn:aws:lambda:<REGION_ID>:367134611783:layer:ballerina-jre21:1

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip
```
