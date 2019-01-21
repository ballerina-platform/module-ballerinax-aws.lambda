import ballerina/http;
import ballerina/io;
import ballerina/system;
import ballerina/time;

# Object to represent an AWS Lambda function execution context.
public type Context object {

    string requestId;
    int deadlineMs;
    string invokedFunctionArn;
    string traceId;

    function __init(string requestId, int deadlineMs, string invokedFunctionArn, string traceId) {
        self.requestId = requestId;
        self.deadlineMs = deadlineMs;
        self.invokedFunctionArn = invokedFunctionArn;
        self.traceId = traceId;
    }

    # Returns the unique id for this request.
    # + return - the request id
    public function getRequestId() returns string {
        return self.requestId;
    }

    # Returns the request execution deadline in milliseconds from the epoch.
    # + return - the request execution deadline
    public function getDeadlineMs() returns int {
        return self.deadlineMs;
    }

    # Returns the ARN of the function being invoked.
    # + return - the invoked function ARN
    public function getInvokedFunctionArn() returns string {
        return self.invokedFunctionArn;
    }

    # Returns the trace id for this request
    # + return - the trace id
    public function getTraceId() returns string {
        return self.traceId;
    }

    # Returns the remaining execution time for this request in milliseconds
    # + return - the remaining execution time
    public function getRemainingExecutionTime() returns int {
        int result = self.deadlineMs - time:currentTime().time;
        if (result < 0) {
            result = 0;
        }
        return result;
    }

};

map<(function (Context, json) returns json|error)> functions = { };
const BASE_URL = "/2018-06-01/runtime/invocation/";

function generateContext(http:Response resp) returns Context {
    string requestId = resp.getHeader("Lambda-Runtime-Aws-Request-Id");
    string deadlineMsStr = resp.getHeader("Lambda-Runtime-Deadline-Ms");
    int deadlineMs = 0;
    var dms = int.convert(deadlineMsStr);
    if (dms is int) {
        deadlineMs = dms;
    }
    string invokedFunctionArn = resp.getHeader("Lambda-Runtime-Invoked-Function-Arn");
    string traceId = resp.getHeader("Lambda-Runtime-Trace-Id");
    Context ctx = new(requestId, deadlineMs, invokedFunctionArn, traceId);
    return ctx;
}

public function __register(string handler, (function (Context, json) returns json|error) func) {
    functions[handler] = func;
}

public function __process() {
    http:Client clientEP = new("http://" + system:getEnv("AWS_LAMBDA_RUNTIME_API"));
    string handlerStr = system:getEnv("_HANDLER");
    string[] hsc = system:getEnv("_HANDLER").split("\\.");
    if (hsc.length() < 2) {
        io:println("Error - invalid handler string: ", handlerStr, ", should be of format {BALX_NAME}.{FUNC_NAME}");
        return;
    }
    string handler = hsc[1];
    var func = functions[handler];
    if (func is (function (Context, json) returns json|error)) {
        while (true) {
            var resp = clientEP->get(BASE_URL + "next");
            if (resp is http:Response) {
                // process each event in its own worker, this will be limited
                // by the total number of worker threads configured for Ballerina
                _ = start processEvent(clientEP, resp, func);
            } else {
                io:println("Error - network failure polling for next event: ", resp);
            }
        }
    } else {
        io:println("Error - invalid handler: ", handler);
    }
}

function processEvent(http:Client clientEP, http:Response resp, (function (Context, json) returns json|error) func) {
    var content = resp.getJsonPayload();
    if (content is json) {
        Context ctx = generateContext(resp);
        http:Request req = new;
        // call the target function, handle any errors if raised by the function
        var funcResp = trap func.call(ctx, content);
        if (funcResp is json) {
            req.setJsonPayload(untaint funcResp);
            // send the response
            _ = clientEP->post(BASE_URL + untaint ctx.requestId + "/response", req);
        } else {
            req.setJsonPayload({errorReasone: funcResp.reason(), 
                                errorMessage: <string> funcResp.detail().message});
            // send the error
            _ = clientEP->post(BASE_URL + untaint ctx.requestId + "/error", req);
        }
    } else {
        io:println("Error - invalid payload: ", resp);
    }
}

