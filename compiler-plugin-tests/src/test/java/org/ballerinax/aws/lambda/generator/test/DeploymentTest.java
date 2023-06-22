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
package org.ballerinax.aws.lambda.generator.test;

import org.ballerinax.aws.lambda.generator.test.utils.BaseTest;
import org.ballerinax.aws.lambda.generator.test.utils.ProcessOutput;
import org.ballerinax.aws.lambda.generator.test.utils.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Test creating awslambda deployment artifacts.
 */
public class DeploymentTest extends BaseTest {
    private Path eventJson;

    @BeforeClass
    public void setup() throws IOException {
        this.eventJson = Files.createTempDirectory("ballerina-aws-lambda-test-").resolve("event.json");
    }
    
    @Test
    public void testAWSLambdaDeployment() throws IOException, InterruptedException {
        Path depedenciesToml = SOURCE_DIR.resolve("deployment").resolve("Dependencies.toml");
        Files.deleteIfExists(depedenciesToml);
        
        ProcessOutput processOutput = TestUtils.compileBallerinaProject(SOURCE_DIR.resolve("deployment"));
        Assert.assertEquals(processOutput.getExitCode(), 0);
        Assert.assertTrue(processOutput.getStdOutput().contains("@aws.lambda"));

        // Check if jar is in .zip
        Path zipFilePath = SOURCE_DIR.resolve("deployment").resolve("target").resolve("bin").resolve("aws-ballerina" +
                "-lambda-functions.zip");
        Assert.assertTrue(Files.exists(zipFilePath));
        URI uri = URI.create("jar:file:" + zipFilePath.toUri().getPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, new HashMap<>())) {
            Path jarFile = zipfs.getPath("/deployment.jar");
            Assert.assertTrue(Files.exists(jarFile));
        }
    }

//    @Test(dependsOnMethods = "testAWSLambdaDeployment", groups = "samCLITest")
//    public void runFunctionTest() throws IOException, InterruptedException {
//        Path srcDirectory = SOURCE_DIR.resolve("deployment");
//
//        String echoContent = "{\"message\":\"Hello World!\"}";
//        FileUtils.write(this.eventJson.toFile(), echoContent, Charset.defaultCharset());
//        ProcessOutput processOutput = TestUtils.runLambdaFunction(srcDirectory, "EchoFunction", this.eventJson);
//        Assert.assertEquals(processOutput.getExitCode(), 0);
//        Assert.assertTrue(processOutput.getStdOutput().contains(echoContent));
//    }

    @AfterClass
    public void cleanUp() throws IOException {
        TestUtils.deleteDirectory(this.eventJson.getParent());
    }
}

