// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/io;
import ballerina/os;
import ballerina/time;
import ballerina/regex;
import ballerina/lang.'decimal;
  
# Object to represent an AWS Lambda function execution context.
public class Context {

    string requestId;
    int deadlineMs;
    string invokedFunctionArn;
    string traceId;

    isolated function init(string requestId, int deadlineMs, string invokedFunctionArn, string traceId) {
        self.requestId = requestId;
        self.deadlineMs = deadlineMs;
        self.invokedFunctionArn = invokedFunctionArn;
        self.traceId = traceId;
    }

    # Returns the unique id for this request.
    # + return - the request id
    public isolated function getRequestId() returns string {
        return self.requestId;
    }

    # Returns the request execution deadline in milliseconds from the epoch.
    # + return - the request execution deadline
    public isolated function getDeadlineMs() returns int {
        return self.deadlineMs;
    }

    # Returns the ARN of the function being invoked.
    # + return - the invoked function ARN
    public isolated function getInvokedFunctionArn() returns string {
        return self.invokedFunctionArn;
    }

    # Returns the trace id for this request
    # + return - the trace id
    public isolated function getTraceId() returns string {
        return self.traceId;
    }

    # Returns the remaining execution time for this request in milliseconds
    # + return - the remaining execution time
    public isolated function getRemainingExecutionTime() returns int {
        int s = self.deadlineMs/1000;
        decimal fs = <decimal>(self.deadlineMs%1000)*1000000;
        time:Utc deadlineUtc = [s,fs];
        time:Utc utc = time:utcNow();
        decimal utcDiff = time:utcDiffSeconds(deadlineUtc, utc);
        int result = <int>'decimal:round(utcDiff*1000);
        if (result < 0) {
            result = 0;
        }
        return result;
    }

}

# Lambda FunctionType
type FunctionType function (Context, anydata) returns json|error;
# Lambda FunctionEntry    
type FunctionEntry [FunctionType, typedesc<anydata>];
map<FunctionEntry> functions = { };
const BASE_URL = "/2018-06-01/runtime/invocation/";

# Generates an AWS Lambda function execution context.
#
# + resp - HTTP Response
# + return - Return AWS Lambda function execution context
isolated function generateContext(http:Response resp) returns @tainted Context {
    string requestId = checkpanic resp.getHeader("Lambda-Runtime-Aws-Request-Id");
    string deadlineMsStr = checkpanic resp.getHeader("Lambda-Runtime-Deadline-Ms");
    int deadlineMs = 0;
    var dms = deadlineMsStr.cloneWithType(int);
    if (dms is int) {
        deadlineMs = dms;
    }
    string invokedFunctionArn = checkpanic resp.getHeader("Lambda-Runtime-Invoked-Function-Arn");
    string traceId = checkpanic resp.getHeader("Lambda-Runtime-Trace-Id");
    Context ctx = new(requestId, deadlineMs, invokedFunctionArn, traceId);
    return ctx;
}

# Register a function handler with function and event type
#
# + handler - Function Hanlder name
# + func - Function type
# + eventType - Event type
public function __register(string handler, FunctionType func, typedesc<anydata> eventType) {
    functions[handler] = [func, eventType];
}

# Convert JSON input to an EventType
#
# + input - Input JSON
# + eventType - Event type
# + return - Returns Event type
isolated function jsonToEventType(json input, typedesc<anydata> eventType) returns anydata|error {
    return input.cloneWithType(eventType);
}

# Process and excute the handler.  
public function __process() {
    http:Client clientEP = checkpanic new("http://" + os:getEnv("AWS_LAMBDA_RUNTIME_API"));
    string handlerStr = os:getEnv("_HANDLER");

    string[] hsc = regex:split(os:getEnv("_HANDLER"), "\\.");
    if (hsc.length() < 2) {
        io:println("Error - invalid handler string: ", handlerStr, ", should be of format {BALX_NAME}.{FUNC_NAME}");
        return;
    }
    string handler = hsc[hsc.length()-1];
    var func = functions[handler];
    if (func is FunctionEntry) {
        while (true) {
            var resp = clientEP->get(BASE_URL + "next");
            if (resp is http:Response) {
                processEvent(clientEP, resp, func);
            } else {
                io:println("Error - network failure polling for next event: ", resp);
            }
        }
    } else {
        io:println("Error - invalid handler: ", handler);
    }
}

# Process call back response.
#
# + clientEP - AWS Lambda URL endpoint
# + resp - Response from AWS Lambda
# + funcEntry - @FunctionEntry 
function processEvent(http:Client clientEP, http:Response resp, FunctionEntry funcEntry) {
    var content = resp.getJsonPayload();
    if (content is json) {
        Context ctx = generateContext(resp);
        http:Request req = new;
        // call the target function, handle any errors if raised by the function
        FunctionType func = funcEntry[0];
        var event = jsonToEventType(content, funcEntry[1]);
        json|error funcResp;
        if event is error {
            funcResp = error("Invalid event type", cause = <@untainted> event);
        } else {
            funcResp = trap func(ctx, event);
        }
        if (funcResp is json) {
            req.setJsonPayload(<@untainted> funcResp);
            // send the response
            var result = clientEP->post(BASE_URL + <@untainted> ctx.requestId + "/response", req);
            if (result is error) {
                io:println("Error - sending response: ", result);
            }
        } else {
            json payload = { errorReason: funcResp.message(), errorDetail: funcResp.detail().toString()};
            req.setJsonPayload(payload);
            // send the error
            var result = clientEP->post(BASE_URL + <@untainted> ctx.requestId + "/error", req);
            if (result is error) {
                io:println("Error - sending error: ", result);
            }
        }
    } else {
        io:println("Error - invalid payload: ", resp);
    }
}
