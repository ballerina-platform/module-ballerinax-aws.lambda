# Ballerina AWS Lambda Extension

Annotation based AWS Lambda extension implementation for Ballerina. 

[![AWS Lambda Build](https://github.com/ballerinax/awslambda/workflows/Ballerinax%20AWS%20Lambda%20Build/badge.svg)](https://github.com/ballerinax/awslambda/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Supported Annotations:

### @awslambda:Function
- Supported with Ballerina package level functions

### Annotation Usage Sample:

```ballerina
import ballerinax/awslambda;
import ballerina/system;
import ballerina/io;

@awslambda:Function
public function echo(awslambda:Context ctx, json input) returns json {
   return input;
}

@awslambda:Function
public function uuid(awslambda:Context ctx, json input) returns json {
   return system:uuid();
}

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
public function notifySQS(awslambda:Context ctx, awslambda:SQSEvent event) returns json {
    return event.Records[0].body;
}

@awslambda:Function
public function notifyS3(awslambda:Context ctx, awslambda:S3Event event) returns json {
    return event.Records[0].s3.'object.key;
}

@awslambda:Function
public function notifyDynamoDB(awslambda:Context ctx, awslambda:DynamoDBEvent event) returns json {
    return event.Records[0].dynamodb.Keys.toString();
}

@awslambda:Function
public function notifySES(awslambda:Context ctx, awslambda:SESEvent event) returns json {
    return event.Records[0].ses.mail.commonHeaders.subject;
}

@awslambda:Function
public function apigwRequest(awslambda:Context ctx, awslambda:APIGatewayProxyRequest request) {
    io:println("Path: ", request.path);
}
```

The output of the Ballerina build is as follows:

```bash
$ ballerina build functions.bal 
Compiling source
	functions.bal

Generating executables
	functions.jar
	@awslambda:Function: echo, uuid, ctxinfo, notifySQS, notifyS3, notifyDynamoDB, notifySES, apigwRequest

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.<FUNCTION_NAME> --runtime provided --role <LAMBDA_ROLE_ARN> --layers arn:aws:lambda:<REGION_ID>:141896495686:layer:ballerina:2

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip
```

