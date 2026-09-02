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
package org.ballerinax.aws.lambda.generator;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton for holding generated lambda functions.
 *
 * @since 2.0.0
 */
public class LambdaFunctionHolder {
    private static LambdaFunctionHolder instance;
    private final List<FunctionDeploymentContext> generatedFunctions;
    private final Map<FunctionDefinitionNode, LambdaFunctionInfo.Destinations> destinations;

    private LambdaFunctionHolder() {
        this.generatedFunctions = new ArrayList<>();
        this.destinations = new IdentityHashMap<>();
    }

    public static LambdaFunctionHolder getInstance() {
        synchronized (LambdaFunctionHolder.class) {
            if (instance == null) {
                instance = new LambdaFunctionHolder();
            }
        }
        return instance;
    }

    public List<FunctionDeploymentContext> getGeneratedFunctions() {
        return this.generatedFunctions;
    }

    /**
     * Destinations by function definition node, for every function declared so far. Held here rather
     * than on the codegen task so that it lives exactly as long as {@code generatedFunctions}: the
     * two are written to the same intermediate file, so a function retained across an invocation
     * must keep its destinations with it.
     *
     * @return the declared destinations
     */
    public Map<FunctionDefinitionNode, LambdaFunctionInfo.Destinations> getDestinations() {
        return this.destinations;
    }
}
