/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinax.awslambda.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Test creating awslambda deployment artifacts.
 */
public class DeploymentTest {

    private static final String DISTRIBUTION_PATH = System.getProperty("ballerina.pack");
    
    private static final String BALLERINA_COMMAND = DISTRIBUTION_PATH + File.separator + "ballerina";
    
    private static final String BUILD = "build";
    
    private static final String SOURCE_DIR = Paths.get("src").resolve("test").resolve("resources").resolve("deployment")
            .toAbsolutePath().toString();
    
    @Test
    public void testAWSLambdaDeployment() throws IOException, InterruptedException {
        // check if the compilation is successful, if artifact generation has an error
        // this compilation will fail
        Assert.assertEquals(compileBallerinaFile(SOURCE_DIR, "functions.bal"), 0);
    }
    
    public static int compileBallerinaFile(String sourceDirectory, String fileName)
            throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(BALLERINA_COMMAND, BUILD, fileName);
        pb.directory(new File(sourceDirectory));        
        Process process = pb.start();
        int exitCode = process.waitFor();
        return exitCode;
    }
    
}

