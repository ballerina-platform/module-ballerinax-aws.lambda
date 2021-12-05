/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.awslambda;

/**
 * Constants for Lambda Functions.
 */
public class Constants {

    public static final String LAMBDA_REG_FUNCTION_NAME = "__register";
    public static final String MAIN_FUNC_NAME = "main";
    public static final String LAMBDA_ORG_NAME = "ballerinax";
    public static final String LAMBDA_MODULE_NAME = "awslambda";
    public static final String LAMBDA_CONTEXT = "Context";
    public static final String CTX_PARAMS_NAME = "ctx";
    public static final String INPUT_PARAMS_NAME = "input";
    public static final String PROXY_FUNCTION_PREFIX = "__func_proxy__";
    public static final String LAMBDA_OUTPUT_ZIP_FILENAME = "aws-ballerina-lambda-functions.zip";
    public static final String AWS_LAMBDA_PREFIX = "aws-lamb";
}
