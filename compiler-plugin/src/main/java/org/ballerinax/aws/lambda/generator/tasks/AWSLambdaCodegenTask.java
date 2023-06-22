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
package org.ballerinax.aws.lambda.generator.tasks;

import com.google.gson.Gson;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.GeneratorTask;
import io.ballerina.projects.plugins.SourceGeneratorContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.ballerina.tools.text.TextRange;
import org.ballerinax.aws.lambda.generator.Constants;
import org.ballerinax.aws.lambda.generator.FunctionDeploymentContext;
import org.ballerinax.aws.lambda.generator.LambdaFunctionExtractor;
import org.ballerinax.aws.lambda.generator.LambdaFunctionHolder;
import org.ballerinax.aws.lambda.generator.LambdaHandlerContainer;
import org.ballerinax.aws.lambda.generator.LambdaUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@code AnalysisTask} that is triggered for AWS Lambda codegen.
 *
 * @since 1.0.0
 */
public class AWSLambdaCodegenTask implements GeneratorTask<SourceGeneratorContext> {

    @Override
    public void generate(SourceGeneratorContext sourceGeneratorContext) {
        Package currentPackage = sourceGeneratorContext.currentPackage();
        LambdaFunctionExtractor lambdaFunctionExtractor = new LambdaFunctionExtractor(currentPackage);
        Module module = currentPackage.getDefaultModule();
        LambdaFunctionHolder functionHolder = LambdaFunctionHolder.getInstance();
        List<FunctionDeploymentContext> generatedFunctions = functionHolder.getGeneratedFunctions();
        for (LambdaHandlerContainer container : lambdaFunctionExtractor.extractFunctions()) {
            for (FunctionDefinitionNode function : container.getFunctions()) {
                FunctionDeploymentContext functionDeploymentContext = new FunctionDeploymentContext(function);
                generatedFunctions.add(functionDeploymentContext);
            }
        }
        try {
            writeObjectToJson(sourceGeneratorContext.currentPackage().project().targetDir(), generatedFunctions);
        } catch (IOException e) {
            DiagnosticInfo
                    diagnosticInfo = new DiagnosticInfo("AWS-Lambda-001", e.getMessage(), DiagnosticSeverity.ERROR);
            sourceGeneratorContext.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    new NullLocation()));
        }
        TextDocument textDocument = generateHandlerDocument(generatedFunctions);
        sourceGeneratorContext.addSourceFile(textDocument, Constants.AWS_LAMBDA_PREFIX, module.moduleId());
    }

    private TextDocument generateHandlerDocument(List<FunctionDeploymentContext> generatedFunctions) {
        FunctionDefinitionNode mainFunction = LambdaUtils.createMainFunction(generatedFunctions);
        ModulePartNode modulePartNode = LambdaUtils.createModulePartNode(generatedFunctions, mainFunction);
        return TextDocuments.from(modulePartNode.toSourceCode());
    }

    private void writeObjectToJson(Path targetPath, List<FunctionDeploymentContext> generatedFunctions)
            throws IOException {
        Gson gson = new Gson();
        Path jsonPath = targetPath.resolve("aws-lambda.json");
        Files.deleteIfExists(jsonPath);
        Files.createFile(jsonPath);
        try (FileWriter r = new FileWriter(jsonPath.toAbsolutePath().toString(), StandardCharsets.UTF_8)) {
            List<String> functionList = new ArrayList<>();
            for (FunctionDeploymentContext ctx : generatedFunctions) {
                functionList.add(ctx.getOriginalFunction().functionName().text());
            }
            gson.toJson(functionList, r);
        }
    }
}

/**
 * Represents Null Location in a ballerina document.
 *
 * @since 2.0.0
 */
class NullLocation implements Location {

    @Override
    public LineRange lineRange() {
        LinePosition from = LinePosition.from(0, 0);
        return LineRange.from("", from, from);
    }

    @Override
    public TextRange textRange() {
        return TextRange.from(0, 0);
    }
}
