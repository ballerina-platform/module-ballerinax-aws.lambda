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

# Represents where AWS Lambda routes the result of an asynchronous invocation.
# Destinations apply to asynchronous invocations only, so a function invoked
# synchronously, such as through a function URL, never routes to them.
#
# + onSuccess - the arn of an SQS queue, SNS topic, EventBridge event bus or Lambda
#               function to route successful invocations to
# + onFailure - the arn of an SQS queue, SNS topic, EventBridge event bus or Lambda
#               function to route failed invocations to
public type DestinationConfig record {|
    string onSuccess?;
    string onFailure?;
|};

# Represents the configuration of an AWS Lambda function. Every field is optional, so
# `@lambda:Function` remains valid with no annotation value.
#
# + destinations - where to route the result of asynchronous invocations
public type FunctionConfig record {|
    DestinationConfig destinations?;
|};

# The annotation, which is used to mark the function as an AWS Lambda function.
public const annotation FunctionConfig Function on function;
