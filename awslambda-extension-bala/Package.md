## Module Overview

This module offers the capabilities of creating AWS Lambda functions using Ballerina. 

- For information on the operations, which you can perform with this module, see [Classes](/learn/api-docs/ballerina/index.html#/ballerinax/awslambda/0.0.0/awslambda/classes/Context). 
- For information on the deployment, see the [AWS Lambda Deployment Guide](/learn/deployment/aws-lambda/).
- For examples on the usage of the operations, see the [AWS Lambda Deployment Example](/learn/by-example/aws-lambda-deployment.html).

### Annotation Usage Sample:

```ballerina
import ballerinax/awslambda;
import ballerina/uuid;
import ballerina/io;

// The `@awslambda:Function` annotation marks a function to
// generate an AWS Lambda function
@awslambda:Function
public function echo(awslambda:Context ctx, json input) returns json {
   return input;
}

@awslambda:Function
public function uuid(awslambda:Context ctx, json input) returns json {
   return uuid:createType1AsString();
}

// The `awslambda:Context` object contains request execution
// context information
@awslambda:Function
public function ctxinfo(awslambda:Context ctx, json input) returns json|error {
   json result = { RequestID: ctx.getRequestId(),
                   DeadlineMS: ctx.getDeadlineMs(),
                   InvokedFunctionArn: ctx.getInvokedFunctionArn(),
                   TraceID: ctx.getTraceId(),
                   RemainingExecTime: ctx.getRemainingExecutionTime() };
   return result;
}

@awslambda:Function
public function notifySQS(awslambda:Context ctx, 
                          awslambda:SQSEvent event) returns json {
    return event.Records[0].body;
}

@awslambda:Function
public function notifyS3(awslambda:Context ctx, 
                         awslambda:S3Event event) returns json {
    return event.Records[0].s3.'object.key;
}

@awslambda:Function
public function notifyDynamoDB(awslambda:Context ctx, 
                               awslambda:DynamoDBEvent event) returns json {
    return event.Records[0].dynamodb.Keys.toString();
}

@awslambda:Function
public function notifySES(awslambda:Context ctx, 
                          awslambda:SESEvent event) returns json {
    return event.Records[0].ses.mail.commonHeaders.subject;
}

@awslambda:Function
public function apigwRequest(awslambda:Context ctx, 
                             awslambda:APIGatewayProxyRequest request) {
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
	@awslambda:Function: echo, uuid, ctxinfo, notifySQS, notifyS3

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name $FUNCTION_NAME --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.$FUNCTION_NAME --runtime provided --role $LAMBDA_ROLE_ARN --layers arn:aws:lambda:$REGION_ID:134633749276:layer:ballerina-jre11:6 --memory-size 512 --timeout 10

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name $FUNCTION_NAME --zip-file fileb://aws-ballerina-lambda-functions.zip
