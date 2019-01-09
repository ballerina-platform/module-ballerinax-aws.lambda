## Sample: functions.bal  

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
```

The output of the Ballerina build is as follows:

```bash
$ ./ballerina build functions.bal 
Compiling source
    functions.bal
Generating executable
    functions.balx
	@awslambda:Function: echo, uuid

	Run the following commands to deploy each Ballerina AWS Lambda function:
	aws lambda create-function --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip --handler functions.<FUNCTION_NAME> --runtime provided --role <LAMBDA_ROLE_ARN> --timeout 10 --memory-size 1024
	aws lambda update-function-configuration --function-name <FUNCTION_NAME> --layers arn:aws:lambda:us-west-2:908363916138:layer:ballerina-0_990_3-runtime:11

	Run the following command to re-deploy an updated Ballerina AWS Lambda function:
	aws lambda update-function-code --function-name <FUNCTION_NAME> --zip-file fileb://aws-ballerina-lambda-functions.zip
```

