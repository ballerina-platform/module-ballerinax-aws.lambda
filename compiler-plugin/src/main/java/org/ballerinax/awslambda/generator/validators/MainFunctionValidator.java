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
package org.ballerinax.awslambda.generator.validators;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.awslambda.generator.Constants;
import org.ballerinax.awslambda.generator.LambdaUtils;

import java.util.List;

/**
 * Responsible for disallowing main function in a ballerina document.
 *
 * @since 2.0.0
 */
public class MainFunctionValidator extends NodeVisitor {

    private final List<Diagnostic> diagnostics;
    private boolean isLambdaFunctionsImportExist = false;

    public MainFunctionValidator(List<Diagnostic> diagnostics) {

        this.diagnostics = diagnostics;
    }

    @Override
    public void visit(ImportDeclarationNode importDeclarationNode) {
        if (LambdaUtils.isAwsLambdaModule(importDeclarationNode)) {
            this.isLambdaFunctionsImportExist = true;
        }
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {

        if (!isLambdaFunctionsImportExist) {
            return;
        }
        String text = functionDefinitionNode.functionName().text();
        if (Constants.MAIN_FUNC_NAME.equals(text)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo("AZ010", "main function is not allowed in " +
                    "lambda functions", DiagnosticSeverity.ERROR);
            diagnostics.add(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.location()));
        }
    }
}
