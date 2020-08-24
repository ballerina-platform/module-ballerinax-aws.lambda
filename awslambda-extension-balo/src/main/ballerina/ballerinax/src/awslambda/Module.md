## Module Overview

This module offers the capabilities of creating AWS Lambda functions using ballerina. 

- For information on the operations, which you can perform with this module, see [Objects](/learn/api-docs/ballerina/awslambda/index.html#objects).
- For information on the deployment, see the [AWS Lambda Deployment Guide](/learn/deployment/aws-lambda/). 
- For examples on the usage of the operations, see the [AWS Lambda Deployment Example](/learn/by-example/aws-lambda-deployment.html).

### Annotation Usage Sample:

```ballerina
import ballerinax/awslambda;
import ballerina/system;

@awslambda:Function
public function echo(awslambda:Context ctx, json input) returns json|error {
   return input;
}

@awslambda:Function
public function uuid(awslambda:Context ctx, json input) returns json|error {
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
```

The output of the Ballerina build is as follows:

```bash
$ ballerina build functions.bal 
Compiling source
    functions.bal

Generating executable
    functions.jar
	@awslambda:Function: echo, uuid, ctxinfo

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.<FUNCTION_NAME> --runtime provided --role <LAMBDA_ROLE_ARN> --layers <BALLERINA_LAYER_ARN>

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip
```
