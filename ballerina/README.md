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

Generating executable
	functions.jar
	@aws.lambda:Function: echo, uuid, ctxinfo, notifySQS, notifyS3

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name $FUNCTION_NAME --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.$FUNCTION_NAME --runtime provided --role $LAMBDA_ROLE_ARN --layers arn:aws:lambda:$REGION_ID:367134611783:layer:ballerina-jre21:1 --memory-size 512 --timeout 10

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
