## Sample: functions.bal  

```ballerina
import ballerinax/aws.lambda;
import ballerina/uuid;

@lambda:Function
public function echo(lambda:Context ctx, json input) returns json|error {
   return input;
}

@lambda:Function
public function uuid(lambda:Context ctx, json input) returns json|error {
   return uuid:createType1AsString();
}

@lambda:Function
public function ctxinfo(lambda:Context ctx, json input) returns json|error {
   json result = { RequestID: ctx.getRequestId(),
                   DeadlineMS: ctx.getDeadlineMs(),
                   InvokedFunctionArn: ctx.getInvokedFunctionArn(),
                   TraceID: ctx.getTraceId(),
                   RemainingExecTime: ctx.getRemainingExecutionTime() };
   return result;
}
```

The output of the bal build is as follows:

```bash
$ ./bal build functions.bal 
Compiling source
    functions.bal
Generating executable
    functions.balx
	@lambda:Function: echo, uuid, ctxinfo

        The Ballerina AWS Lambda layer information can be found at https://ballerina.io/deployment/aws-lambda.

	Run the following command to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.<FUNCTION_NAME> --runtime provided --role <LAMBDA_ROLE_ARN> --layers <BALLERINA_LAYER_ARN>

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip
```

