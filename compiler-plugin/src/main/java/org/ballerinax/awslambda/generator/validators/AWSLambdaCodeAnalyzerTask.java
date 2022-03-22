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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinax.awslambda.generator.Constants;

import java.util.ArrayList;
import java.util.List;

/***
 * Code analyzer for azure function specific validations.
 *
 * @since 2.0.0
 */
public class AWSLambdaCodeAnalyzerTask implements AnalysisTask<CompilationAnalysisContext> {

    @Override
    public void perform(CompilationAnalysisContext compilationAnalysisContext) {
        Package currentPackage = compilationAnalysisContext.currentPackage();
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (currentPackage.project().kind() != ProjectKind.BUILD_PROJECT) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo("A000", "lambda functions are only allowed in " +
                    "ballerina projects", DiagnosticSeverity.ERROR);
            DocumentId firsDocument = currentPackage.getDefaultModule().documentIds().iterator().next();
            Document document = currentPackage.getDefaultModule().document(firsDocument);
            NodeLocation location = document.syntaxTree().rootNode().location();
            diagnostics.add(DiagnosticFactory.createDiagnostic(diagnosticInfo, location));
        }

        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                Node rootNode = document.syntaxTree().rootNode();
                if (document.name().startsWith(Constants.AWS_LAMBDA_PREFIX)) {
                    continue;
                }
                diagnostics.addAll(validateMainFunction(rootNode));
                if (module.isDefaultModule()) {
                    continue;
                }
                diagnostics.addAll(validateSubmoduleDocument(rootNode));
            }
        }
        diagnostics.forEach(compilationAnalysisContext::reportDiagnostic);
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
