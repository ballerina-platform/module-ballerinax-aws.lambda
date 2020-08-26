import ballerinax/awslambda;
import ballerina/system;

// The `@awslambda:Function` annotation marks a function to
// generate an AWS Lambda function
@awslambda:Function
public function echo(awslambda:Context ctx, json input) returns json|error {
   return input;
}

@awslambda:Function
public function uuid(awslambda:Context ctx, json input) returns json|error {
   return system:uuid();
}

@awslambda:Function
// The `awslambda:Context` object contains request execution
// context information
public function ctxinfo(awslambda:Context ctx, json input) returns json|error {
   json result = { RequestID: ctx.getRequestId(),
                   DeadlineMS: ctx.getDeadlineMs(),
                   InvokedFunctionArn: ctx.getInvokedFunctionArn(),
                   TraceID: ctx.getTraceId(),
                   RemainingExecTime: ctx.getRemainingExecutionTime() };
   return result;
}

@awslambda:Function
public function notifySQS(awslambda:Context ctx, awslambda:SQSEvent event) returns json|error {
    return event.Records[0].body;
}

@awslambda:Function
public function notifyS3(awslambda:Context ctx, awslambda:S3Event event) returns json|error {
    return event.Records[0].s3.'object.key;
}
