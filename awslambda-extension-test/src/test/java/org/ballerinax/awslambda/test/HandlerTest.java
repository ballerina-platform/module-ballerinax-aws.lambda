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
package org.ballerinax.awslambda.test;

import com.google.gson.JsonObject;
import io.ballerina.projects.CodeGeneratorResult;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import org.ballerinax.awslambda.test.utils.ParserTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test case for checking generated source of handler.
 */
public class HandlerTest {
    protected static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/handlers/");
    @Test
    public void testGeneratedHandlerSource() {
        try {
            BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("code"));
            CodeGeneratorResult codeGeneratorResult = project.currentPackage().runCodeGeneratorPlugins();
            Package updatedPackage = codeGeneratorResult.updatedPackage().orElseThrow();
            PackageCompilation compilation = updatedPackage.getCompilation();
            DiagnosticResult diagnosticResult = compilation.diagnosticResult();
            Assert.assertFalse(diagnosticResult.hasErrors());
            Module module = project.currentPackage().getDefaultModule();
            Assert.assertEquals(module.documentIds().size(), 2);
            for (DocumentId documentId : module.documentIds()) {
                Document document = module.document(documentId);
                if (document.name().equals("main.bal")) {
                    continue;
                }

                JsonObject assertJson =
                        ParserTestUtils.readAssertFile(RESOURCE_DIRECTORY.resolve("generated").resolve("generated" +
                        ".json"));

                // Validate the tree against the assertion file
                ParserTestUtils.assertNode(document.syntaxTree().rootNode().internalNode(), assertJson);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
