@echo off

REM ---------------------------------------------------------------------------
REM   Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
REM
REM   Licensed under the Apache License, Version 2.0 (the "License");
REM   you may not use this file except in compliance with the License.
REM   You may obtain a copy of the License at
REM
REM   http://www.apache.org/licenses/LICENSE-2.0
REM
REM   Unless required by applicable law or agreed to in writing, software
REM   distributed under the License is distributed on an "AS IS" BASIS,
REM   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM   See the License for the specific language governing permissions and
REM   limitations under the License.

set DISTRIBUTION_PATH=%1
set AWSLAMBDA_ANNOTATIONS_MAVEN_PROJECT_ROOT=%2

set EXECUTABLE=%DISTRIBUTION_PATH%\bin\ballerina
set AWSLAMBDA_BALLERINA_PROJECT=%AWSLAMBDA_ANNOTATIONS_MAVEN_PROJECT_ROOT%\src\main\ballerina\
set DISTRIBUTION_BIR_CACHE=%DISTRIBUTION_PATH%\bir-cache\ballerinax\awslambda\0.0.0\
set DISTRIBUTION_SYSTEM_LIB=%DISTRIBUTION_PATH%\bre\lib\

if not exist "%DISTRIBUTION_BIR_CACHE%" mkdir "%DISTRIBUTION_BIR_CACHE%"
if not exist "%DISTRIBUTION_SYSTEM_LIB%" mkdir "%DISTRIBUTION_SYSTEM_LIB%"

pushd %AWSLAMBDA_BALLERINA_PROJECT%
cmd /C %EXECUTABLE% clean
set JAVA_OPTS="-DBALLERINA_DEV_COMPILE_BALLERINA_ORG=true"
cmd /C %EXECUTABLE% build -c -a --skip-tests
popd

COPY "%AWSLAMBDA_BALLERINA_PROJECT%target\caches\bir_cache\ballerinax\awslambda\0.0.0\awslambda.bir" "%DISTRIBUTION_BIR_CACHE%"
COPY "%AWSLAMBDA_BALLERINA_PROJECT%Ballerina.toml" "%DISTRIBUTION_BIR_CACHE%"
COPY "%AWSLAMBDA_BALLERINA_PROJECT%target\caches\jar_cache\ballerinax\awslambda\ballerinax-awslambda-0.0.0.jar" "%DISTRIBUTION_SYSTEM_LIB%"
