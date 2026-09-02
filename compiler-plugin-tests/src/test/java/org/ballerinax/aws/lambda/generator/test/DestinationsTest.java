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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test reading destinations from the {@code @lambda:Function} annotation and emitting the command
 * that configures them.
 */
public class DestinationsTest extends BaseTest {

    private static final Path PROJECT = SOURCE_DIR.resolve("destinations");
    private static final String SUCCESS_ARN = "arn:aws:sqs:us-west-1:123456789012:success-queue";
    private static final String FAILURE_ARN = "arn:aws:sns:us-west-1:123456789012:failure-topic";

    private String buildOutput;

    @BeforeClass
    public void build() throws IOException, InterruptedException {

        Files.deleteIfExists(PROJECT.resolve("Dependencies.toml"));
        ProcessOutput processOutput = TestUtils.compileBallerinaProject(PROJECT);
        Assert.assertEquals(processOutput.getExitCode(), 0, processOutput.getErrOutput());
        this.buildOutput = processOutput.getStdOutput();
    }

    /**
     * The annotation type has only optional fields, so attaching it with no value at all has to
     * stay valid. Every existing package written against this module does exactly that.
     */
    @Test
    public void testAnnotationRemainsValidWithoutAValue() {

        Assert.assertTrue(this.buildOutput.contains("noConfig"), "a function with no annotation value " +
                "should still be registered");
        Assert.assertTrue(this.buildOutput.contains("emptyConfig"), "a function with an empty annotation " +
                "value should still be registered");
    }

    @Test
    public void testBothDestinationsAreEmitted() {

        Assert.assertTrue(this.buildOutput.contains(
                        "put-function-event-invoke-config --function-name bothDestinations"),
                "a function declaring destinations should get a put-function-event-invoke-config command");
        Assert.assertTrue(this.buildOutput.contains("\"OnSuccess\":{\"Destination\":\"" + SUCCESS_ARN + "\"}"));
        Assert.assertTrue(this.buildOutput.contains("\"OnFailure\":{\"Destination\":\"" + FAILURE_ARN + "\"}"));
    }

    /**
     * A function can route only failures, so the emitted config must omit the key that was not
     * given rather than sending an empty destination.
     */
    @Test
    public void testOnlyFailureIsEmittedWhenOnlyFailureIsDeclared() {

        String config = destinationConfigOf("failureOnly");
        Assert.assertTrue(config.contains("OnFailure"));
        Assert.assertFalse(config.contains("OnSuccess"), "OnSuccess must be absent when it was not declared");
    }

    /**
     * The mirror of the above. The two keys are joined by a comma when both are present, so
     * declaring only the first exercises a different branch from declaring only the second.
     */
    @Test
    public void testOnlySuccessIsEmittedWhenOnlySuccessIsDeclared() {

        String config = destinationConfigOf("successOnly");
        Assert.assertTrue(config.contains("OnSuccess"));
        Assert.assertFalse(config.contains("OnFailure"), "OnFailure must be absent when it was not declared");
        Assert.assertFalse(config.contains(",,"), "the config must not contain an empty element");
        Assert.assertTrue(config.endsWith("\"}}"), "the config must be closed properly, was: " + config);
    }

    /**
     * Returns the --destination-config argument emitted for a function.
     */
    private String destinationConfigOf(String functionName) {

        int index = this.buildOutput.indexOf("--function-name " + functionName + " ");
        Assert.assertTrue(index > -1, functionName + " should get a destinations command");
        String command = this.buildOutput.substring(index);
        int start = command.indexOf("--destination-config '") + "--destination-config '".length();
        int end = command.indexOf('\'', start);
        return command.substring(start, end);
    }

    @Test
    public void testFunctionsWithoutDestinationsGetNoCommand() {

        Assert.assertFalse(this.buildOutput.contains("--function-name noConfig"),
                "a function without destinations should not get a destinations command");
        Assert.assertFalse(this.buildOutput.contains("--function-name emptyConfig"),
                "a function with an empty annotation value should not get a destinations command");
    }

    /**
     * The intermediate file carries the configuration from code generation through to the point
     * where the deploy instructions are built.
     */
    @Test
    public void testIntermediateJsonCarriesDestinations() throws IOException {

        Path json = PROJECT.resolve("target").resolve("aws-lambda.json");
        Assert.assertTrue(Files.exists(json));
        String content = Files.readString(json, Charset.defaultCharset());

        Assert.assertTrue(content.contains("{\"name\":\"noConfig\"}"),
                "a function without destinations should carry no destinations key, was: " + content);
        Assert.assertTrue(content.contains("\"onSuccess\":\"" + SUCCESS_ARN + "\""));
        Assert.assertTrue(content.contains("\"onFailure\":\"" + FAILURE_ARN + "\""));
    }
}
