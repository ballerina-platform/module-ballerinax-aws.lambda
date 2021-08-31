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

import io.ballerina.projects.plugins.CompilerLifecycleEventContext;
import io.ballerina.projects.plugins.CompilerLifecycleTask;
import org.ballerinalang.core.util.exceptions.BallerinaException;
import org.ballerinax.awslambda.Constants;
import org.ballerinax.awslambda.FunctionDeploymentContext;
import org.ballerinax.awslambda.LambdaFunctionHolder;
import org.ballerinax.awslambda.LambdaUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contains the code generation part of the lambda functions.
 *
 * @since 2.0.0
 */
public class LambdaCodeGeneratedTask implements CompilerLifecycleTask<CompilerLifecycleEventContext> {

    private static final PrintStream OUT = System.out;

    @Override
    public void perform(CompilerLifecycleEventContext compilerLifecycleEventContext) {
        List<FunctionDeploymentContext> generatedFunctions =
                LambdaFunctionHolder.getInstance().getGeneratedFunctions();
        if (generatedFunctions.isEmpty()) {
            // no lambda functions, nothing else to do
            return;
        }
        OUT.println("\t@awslambda:Function: " + String.join(", ", LambdaUtils.getFunctionList(generatedFunctions)));
        Optional<Path> generatedArtifactPath = compilerLifecycleEventContext.getGeneratedArtifactPath();
        if (generatedArtifactPath.isPresent()) {
            Path executablePath = generatedArtifactPath.get();
            String balxName;
            try {
                this.generateZipFile(executablePath);
            } catch (IOException e) {
                throw new BallerinaException("Error generating AWS lambda zip file: " + e.getMessage(), e);
            }
            String fileName = executablePath.getFileName().toString();
            balxName = fileName.substring(0, fileName.lastIndexOf('.'));
            OUT.println("\n\tRun the following command to deploy each Ballerina AWS Lambda function:");
            Path parent = executablePath.getParent();
            OUT.println("\taws lambda create-function --function-name $FUNCTION_NAME --zip-file fileb://"
                    + parent.toString() + File.separator + Constants.LAMBDA_OUTPUT_ZIP_FILENAME + " --handler " +
                    balxName
                    + ".$FUNCTION_NAME --runtime provided --role $LAMBDA_ROLE_ARN --layers "
                    + "arn:aws:lambda:$REGION_ID:" + Constants.AWS_BALLERINA_LAYER + " --memory-size 512 --timeout 10");
            OUT.println("\n\tRun the following command to re-deploy an updated Ballerina AWS Lambda function:");
            OUT.println("\taws lambda update-function-code --function-name $FUNCTION_NAME --zip-file fileb://"
                    + Constants.LAMBDA_OUTPUT_ZIP_FILENAME + "\n\n");
        }
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
