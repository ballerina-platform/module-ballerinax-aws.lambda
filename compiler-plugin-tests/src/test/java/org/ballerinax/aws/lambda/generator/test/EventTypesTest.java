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
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the ballerina tests that check the event and function URL record definitions against payloads
 * AWS actually delivers. The payloads were captured from real SNS, EventBridge, Kinesis and function
 * URL invocations, because AWS's documented sample payloads differ from what it sends.
 */
public class EventTypesTest extends BaseTest {

    private static final Path PROJECT = SOURCE_DIR.resolve("events");

    @Test
    public void testEventTypesAgainstCapturedPayloads() throws IOException, InterruptedException {

        Files.deleteIfExists(PROJECT.resolve("Dependencies.toml"));

        ProcessOutput processOutput = TestUtils.testBallerinaProject(PROJECT);
        String output = processOutput.getStdOutput() + processOutput.getErrOutput();
        Assert.assertEquals(processOutput.getExitCode(), 0, "the ballerina tests failed: " + output);
        Assert.assertFalse(output.contains("[fail]"), "a ballerina test failed: " + output);
        Assert.assertTrue(output.contains("passing"), "no ballerina tests ran, output was: " + output);
    }
}
