// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerinax/awslambda;
import ballerina/uuid;
import ballerina/io;

@awslambda:Function
public function echo(awslambda:Context ctx, json input) returns json {
   return input;
}

@awslambda:Function
public function uuid(awslambda:Context ctx, json input) returns json {
   return uuid:createType1AsString();
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
