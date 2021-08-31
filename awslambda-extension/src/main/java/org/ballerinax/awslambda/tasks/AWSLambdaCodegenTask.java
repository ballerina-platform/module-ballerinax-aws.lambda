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
package org.ballerinax.awslambda.tasks;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinax.awslambda.Constants;
import org.ballerinax.awslambda.FunctionDeploymentContext;
import org.ballerinax.awslambda.LambdaFunctionExtractor;
import org.ballerinax.awslambda.LambdaFunctionHolder;
import org.ballerinax.awslambda.LambdaHandlerContainer;
import org.ballerinax.awslambda.LambdaUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@code AnalysisTask} that is triggered for AWS Lambda codegen.
 *
 * @since 1.0.0
 */
public class AWSLambdaCodegenTask implements AnalysisTask<CompilationAnalysisContext> {

    @Override
    public void perform(CompilationAnalysisContext compilationAnalysisContext) {
        Package currentPackage = compilationAnalysisContext.currentPackage();
        LambdaFunctionExtractor lambdaFunctionExtractor = new LambdaFunctionExtractor(currentPackage);
        List<Diagnostic> diagnostics = new ArrayList<>(lambdaFunctionExtractor.validateModules());
        Module module = currentPackage.getDefaultModule();
        LambdaFunctionHolder functionHolder = LambdaFunctionHolder.getInstance();
        List<FunctionDeploymentContext> generatedFunctions = functionHolder.getGeneratedFunctions();
        for (LambdaHandlerContainer container : lambdaFunctionExtractor.extractFunctions()) {
            for (FunctionDefinitionNode function : container.getFunctions()) {
                FunctionDeploymentContext functionDeploymentContext = new FunctionDeploymentContext(function);
                generatedFunctions.add(functionDeploymentContext);
            }
        }
        DocumentConfig documentConfig =
                generateIntermediateHandlerDocument(currentPackage.project(), generatedFunctions);
        //Used to avoid duplicate documents as codeAnalyze is getting called multiple times
        if (!LambdaUtils.isDocumentExistInModule(module, documentConfig)) {
            module.modify().addDocument(documentConfig).apply();
            currentPackage.getCompilation();
        }
        for (Diagnostic diagnostic : diagnostics) {
            compilationAnalysisContext.reportDiagnostic(diagnostic);
        }
    }

    private DocumentConfig generateIntermediateHandlerDocument(Project project,
                                                               List<FunctionDeploymentContext> generatedFunctions) {
        Module module = project.currentPackage().getDefaultModule();
        FunctionDefinitionNode mainFunction = LambdaUtils.createMainFunction(generatedFunctions);
        ModulePartNode modulePartNode = LambdaUtils.createModulePartNode(generatedFunctions, mainFunction);
        String newFileContent = modulePartNode.toSourceCode();
        String fileName = module.moduleName().toString() + "-" + Constants.GENERATED_FILE_NAME;
        Path filePath = project.sourceRoot().resolve(fileName);
        DocumentId newDocumentId = DocumentId.create(filePath.toString(), module.moduleId());
        return DocumentConfig.from(newDocumentId, newFileContent, fileName);
    }
}
