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

package org.ballerinax.awslambda.generator;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for Extracting lambda functions from the project.
 *
 * @since 2.0.0
 */
public class LambdaFunctionExtractor {

    private final Package currentPackage;

    public LambdaFunctionExtractor(Package currentPackage) {
        this.currentPackage = currentPackage;
    }

    public List<LambdaHandlerContainer> extractFunctions() {
        Module module = this.currentPackage.getDefaultModule();
        List<LambdaHandlerContainer> handlerList = new ArrayList<>();
        for (DocumentId documentId : module.documentIds()) {
            Document document = module.document(documentId);
            Node node = document.syntaxTree().rootNode();
            if (document.name().startsWith(Constants.AWS_LAMBDA_PREFIX)) {
                continue;
            }
            SemanticModel semanticModel = module.getCompilation().getSemanticModel();
            LambdaFunctionVisitor lambdaFunctionVisitor = new LambdaFunctionVisitor(semanticModel);
            node.accept(lambdaFunctionVisitor);
            List<FunctionDefinitionNode> functions = lambdaFunctionVisitor.getFunctions();
            handlerList.add(new LambdaHandlerContainer(functions));
        }
        return handlerList;
    }
}
