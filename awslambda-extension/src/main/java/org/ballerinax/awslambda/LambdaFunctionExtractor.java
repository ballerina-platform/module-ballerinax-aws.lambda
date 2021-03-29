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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinax.awslambda.validators.MainFunctionValidator;
import org.ballerinax.awslambda.validators.SubmoduleValidator;

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
            SemanticModel semanticModel = module.getCompilation().getSemanticModel();
            LambdaFunctionVisitor lambdaFunctionVisitor = new LambdaFunctionVisitor(semanticModel);
            node.accept(lambdaFunctionVisitor);
            List<FunctionDefinitionNode> functions = lambdaFunctionVisitor.getFunctions();
            handlerList.add(new LambdaHandlerContainer(functions));
        }
        return handlerList;
    }

    public List<Diagnostic> validateModules() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ModuleId moduleId : this.currentPackage.moduleIds()) {
            Module module = this.currentPackage.module(moduleId);
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                Node rootNode = document.syntaxTree().rootNode();
                if (document.name().endsWith(Constants.GENERATED_FILE_NAME)) {
                    continue;
                }
                diagnostics.addAll(validateMainFunction(rootNode));
                if (module.isDefaultModule()) {
                    continue;
                }
                diagnostics.addAll(validateSubmoduleDocument(rootNode));
            }
        }
        return diagnostics;
    }

    private List<Diagnostic> validateMainFunction(Node node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        node.accept(new MainFunctionValidator(diagnostics));
        return diagnostics;
    }

    private List<Diagnostic> validateSubmoduleDocument(Node node) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        node.accept(new SubmoduleValidator(diagnostics));
        return diagnostics;
    }
}
