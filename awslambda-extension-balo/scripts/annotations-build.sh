#
# Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
# WSO2 Inc. licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except
# in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#!/usr/bin/env bash

DISTRIBUTION_PATH=${1}
AWSLAMBDA_BALO_MAVEN_PROJECT_ROOT=${2}

EXECUTABLE="${DISTRIBUTION_PATH}/bin/ballerina"
AWSLAMBDA_BALLERINA_PROJECT="${AWSLAMBDA_BALO_MAVEN_PROJECT_ROOT}/src/main/ballerina"
DISTRIBUTION_BIR_CACHE="${DISTRIBUTION_PATH}/bir-cache/ballerinax/awslambda/0.0.0/"
DISTRIBUTION_SYSTEM_LIB="${DISTRIBUTION_PATH}/bre/lib/"

mkdir -p ${DISTRIBUTION_BIR_CACHE}
mkdir -p ${DISTRIBUTION_SYSTEM_LIB}

if ! hash pushd 2>/dev/null
then
    cd ${AWSLAMBDA_BALLERINA_PROJECT}
    ${EXECUTABLE} clean
    JAVA_OPTS="-DBALLERINA_DEV_COMPILE_BALLERINA_ORG=true" ${EXECUTABLE} build -c -a --skip-tests
    cd -
else
    pushd ${AWSLAMBDA_BALLERINA_PROJECT} /dev/null 2>&1
      ${EXECUTABLE} clean
      JAVA_OPTS="-DBALLERINA_DEV_COMPILE_BALLERINA_ORG=true" ${EXECUTABLE} build -c -a --skip-tests
    popd > /dev/null 2>&1
fi

cp ${AWSLAMBDA_BALLERINA_PROJECT}/target/caches/bir_cache/ballerinax/awslambda/0.0.0/awslambda.bir ${DISTRIBUTION_BIR_CACHE}
cp ${AWSLAMBDA_BALLERINA_PROJECT}/Ballerina.toml ${DISTRIBUTION_BIR_CACHE}
cp ${AWSLAMBDA_BALLERINA_PROJECT}/target/caches/jar_cache/ballerinax/awslambda/0.0.0/ballerinax-awslambda-0.0.0.jar ${DISTRIBUTION_SYSTEM_LIB}
