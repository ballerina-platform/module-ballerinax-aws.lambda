/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinax.aws.lambda.generator;

/**
 * A lambda function and the configuration given on its {@code @lambda:Function} annotation.
 * Serialised to the intermediate aws-lambda.json so the deployment instructions can be built
 * after the executable is generated.
 *
 * @since 3.4.0
 */
public class LambdaFunctionInfo {

    private final String name;
    private final Destinations destinations;

    public LambdaFunctionInfo(String name, Destinations destinations) {
        this.name = name;
        this.destinations = destinations;
    }

    public String getName() {
        return this.name;
    }

    public Destinations getDestinations() {
        return this.destinations;
    }

    /**
     * Where AWS Lambda routes the result of an asynchronous invocation. Either field may be
     * absent, as a function can route only successes, only failures, or neither.
     *
     * @since 3.4.0
     */
    public static class Destinations {

        private final String onSuccess;
        private final String onFailure;

        public Destinations(String onSuccess, String onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        public String getOnSuccess() {
            return this.onSuccess;
        }

        public String getOnFailure() {
            return this.onFailure;
        }

        public boolean isEmpty() {
            return this.onSuccess == null && this.onFailure == null;
        }
    }
}
