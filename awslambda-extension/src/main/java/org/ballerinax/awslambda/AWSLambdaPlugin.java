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

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.compiler.plugins.AbstractCompilerPlugin;
import org.ballerinalang.compiler.plugins.SupportedAnnotationPackages;
import org.ballerinalang.core.util.exceptions.BallerinaException;
import org.ballerinalang.model.tree.PackageNode;
import org.ballerinalang.util.diagnostic.DiagnosticLog;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiler plugin to process AWS lambda function annotations.
 */
@SupportedAnnotationPackages(value = "ballerinax/awslambda:0.0.0")
public class AWSLambdaPlugin extends AbstractCompilerPlugin {

    private static final PrintStream OUT = System.out;

    private static List<FunctionDeploymentContext> generatedFunctions = new ArrayList<>();

    private DiagnosticLog dlog;

    @Override
    public void init(DiagnosticLog diagnosticLog) {
        this.dlog = diagnosticLog;
    }

    @Override
    public void process(PackageNode packageNode) {
        super.process(packageNode);
    }

    @Override
    public List<Diagnostic> codeAnalyze(Project project) {
        LambdaFunctionExtractor lambdaFunctionExtractor = new LambdaFunctionExtractor(project);
        List<Diagnostic> diagnostics = new ArrayList<>(lambdaFunctionExtractor.validateModules());
        Module module = project.currentPackage().getDefaultModule();
        for (LambdaHandlerContainer container : lambdaFunctionExtractor.extractFunctions()) {
            for (FunctionDefinitionNode function : container.getFunctions()) {
                FunctionDeploymentContext functionDeploymentContext = new FunctionDeploymentContext(function);
                generatedFunctions.add(functionDeploymentContext);
            }
        }
        DocumentConfig documentConfig = generateIntermediateHandlerDocument(project, module);
        //Used to avoid duplicate documents as codeAnalyze is getting called multiple times
        if (!LambdaUtils.isDocumentExistInModule(module, documentConfig)) {
            module.modify().addDocument(documentConfig).apply();
            project.currentPackage().getCompilation();
        }
        return diagnostics;
    }

    private DocumentConfig generateIntermediateHandlerDocument(Project project, Module module) {
        FunctionDefinitionNode mainFunction = LambdaUtils.createMainFunction(generatedFunctions);
        ModulePartNode modulePartNode = LambdaUtils.createModulePartNode(generatedFunctions, mainFunction);
        String newFileContent = modulePartNode.toSourceCode();
        String fileName = module.moduleName().toString() + "-" + Constants.GENERATED_FILE_NAME;
        Path filePath = project.sourceRoot().resolve(fileName);
        DocumentId newDocumentId = DocumentId.create(filePath.toString(), module.moduleId());
        return DocumentConfig.from(newDocumentId, newFileContent, fileName);
    }

    @Override
    public void codeGenerated(Project project, Target target) {
        if (AWSLambdaPlugin.generatedFunctions.isEmpty()) {
            // no lambda functions, nothing else to do
            return;
        }
        OUT.println("\t@awslambda:Function: " + String.join(", ", LambdaUtils.getFunctionList(generatedFunctions)));
        String balxName;
        try {
            String fileName = target.getExecutablePath(project.currentPackage()).getFileName().toString();
            balxName = fileName.substring(0, fileName.lastIndexOf('.'));

            this.generateZipFile(target.getExecutablePath(project.currentPackage()));
        } catch (IOException e) {
            throw new BallerinaException("Error generating AWS lambda zip file: " + e.getMessage(), e);
        }
        OUT.println("\n\tRun the following command to deploy each Ballerina AWS Lambda function:");
        try {
            OUT.println("\taws lambda create-function --function-name $FUNCTION_NAME --zip-file fileb://"
                    + target.getExecutablePath(project.currentPackage()).getParent().toString() + File.separator
                    + Constants.LAMBDA_OUTPUT_ZIP_FILENAME + " --handler " + balxName
                    + ".$FUNCTION_NAME --runtime provided --role $LAMBDA_ROLE_ARN --layers "
                    + "arn:aws:lambda:$REGION_ID:" + Constants.AWS_BALLERINA_LAYER + " --memory-size 512 --timeout 10");
        } catch (IOException e) {
            //ignored
        }
        OUT.println("\n\tRun the following command to re-deploy an updated Ballerina AWS Lambda function:");
        OUT.println("\taws lambda update-function-code --function-name $FUNCTION_NAME --zip-file fileb://"
                + Constants.LAMBDA_OUTPUT_ZIP_FILENAME + "\n\n");
    }

    private void generateZipFile(Path binaryPath) throws IOException {
        Path path = binaryPath.toAbsolutePath().getParent().resolve(Constants.LAMBDA_OUTPUT_ZIP_FILENAME);
        Files.deleteIfExists(path);
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:file:" + path.toUri().getPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path pathInZipfile = zipfs.getPath("/" + binaryPath.getFileName());
            Files.copy(binaryPath, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
