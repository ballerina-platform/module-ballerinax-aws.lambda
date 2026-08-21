/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test packaging awslambda functions as a container image. Requires Docker, as the native image
 * tests already do.
 */
public class ContainerImageTest extends BaseTest {

    private static final String IMAGE = "deployment:0.1.0";
    private static final Path PROJECT = SOURCE_DIR.resolve("deployment");

    @Test
    public void testContainerImageBuild() throws IOException, InterruptedException {

        Files.deleteIfExists(PROJECT.resolve("Dependencies.toml"));

        ProcessOutput processOutput = TestUtils.compileBallerinaProject(PROJECT, "--cloud=aws_lambda_image");
        Assert.assertEquals(processOutput.getExitCode(), 0);
        String out = processOutput.getStdOutput();
        Assert.assertTrue(out.contains("@aws.lambda"));
        Assert.assertTrue(out.contains("Built the container image " + IMAGE),
                "the build should report the image it produced");

        // The ZIP artifact must not be produced when building an image.
        Path zip = PROJECT.resolve("target").resolve("aws_lambda")
                .resolve("aws-ballerina-lambda-functions.zip");
        Assert.assertFalse(Files.exists(zip), "an image build should not also emit a ZIP package");
    }

    @Test(dependsOnMethods = "testContainerImageBuild")
    public void testGeneratedDockerfile() throws IOException {

        Path dockerfile = PROJECT.resolve("target").resolve("aws_lambda").resolve("Dockerfile");
        Assert.assertTrue(Files.exists(dockerfile), "a Dockerfile should be generated");
        String content = Files.readString(dockerfile, Charset.defaultCharset());

        Assert.assertTrue(content.contains("FROM public.ecr.aws/lambda/java:21"),
                "a JVM build should use the Lambda Java base image");
        Assert.assertTrue(content.contains("COPY deployment.jar /var/task/"),
                "the built jar should be copied to the task root");
        // Lambda passes the image config command as arguments to the entrypoint rather than in
        // _HANDLER, and the generated Ballerina main accepts no operands, so the entrypoint has to
        // move the argument into _HANDLER. Without this the runtime exits before serving a request.
        Assert.assertTrue(content.contains("export _HANDLER=$1"),
                "the entrypoint must move the image config command into _HANDLER");
        Assert.assertFalse(content.contains("CMD "),
                "the handler must not be baked into the image, so one image can serve every function");
    }

    @Test(dependsOnMethods = "testContainerImageBuild")
    public void testDeployInstructions() throws IOException, InterruptedException {

        ProcessOutput processOutput = TestUtils.compileBallerinaProject(PROJECT, "--cloud=aws_lambda_image");
        String out = processOutput.getStdOutput();

        // A push to a repository that was never created fails, so the instructions have to create it.
        Assert.assertTrue(out.contains("aws ecr create-repository --repository-name deployment"),
                "the instructions must create the ECR repository before pushing");
        Assert.assertTrue(out.contains("--package-type Image"));
        // The AWS CLI rejects a lowercase key: "Unknown parameter in ImageConfig: command, must be
        // one of: EntryPoint, Command, WorkingDirectory".
        Assert.assertTrue(out.contains("{\"Command\":["), "the image config key must be capitalised");
        Assert.assertFalse(out.contains("{\"command\":["), "a lowercase image config key is rejected by the CLI");
    }

    /**
     * Lambda rejects an OCI image index with "The image manifest, config or layer media type for
     * the source image is not supported". BuildKit produces one by default because it attaches
     * provenance and SBOM attestations, so the build has to opt out of both.
     */
    @Test(dependsOnMethods = "testContainerImageBuild")
    public void testImageManifestIsAcceptedByLambda() throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder("docker", "image", "inspect", IMAGE,
                "--format", "{{.Descriptor.MediaType}}");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String mediaType;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            mediaType = reader.readLine();
        }
        Assert.assertEquals(process.waitFor(), 0, "docker image inspect should succeed");
        Assert.assertNotNull(mediaType);
        Assert.assertFalse(mediaType.contains("image.index"),
                "an OCI image index is rejected by Lambda, got " + mediaType);
    }

    @AfterClass
    public void cleanUp() throws IOException, InterruptedException {

        new ProcessBuilder("docker", "rmi", "-f", IMAGE).start().waitFor();
    }
}
