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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Contains the project related validations of lambda functions.
 *
 * @since 2.0.0
 */
public class ProjectValidationTest {

    protected static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/validations/");
    @Test
    public void mainFunctionTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("main"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = diagnosticResult.errors().iterator().next();
        Assert.assertEquals(diagnostic.message(), "main function is not allowed in lambda functions");
    }
    
    @Test
    public void submoduleTest() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY.resolve("submodule"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Iterator<Diagnostic> iterator = diagnosticResult.errors().iterator();
        Diagnostic unusedModuleDiag = iterator.next();
        Assert.assertEquals(unusedModuleDiag.message(), "unused module prefix 'mod1'");
        Diagnostic submoduleDiag = iterator.next();
        Assert.assertEquals(submoduleDiag.message(), "lambda functions is not allowed inside sub modules");
    }

    @Test
    public void singleFileTest() {
        SingleFileProject project = SingleFileProject.load(RESOURCE_DIRECTORY.resolve("single-file").resolve(
                "functions.bal"));
        PackageCompilation compilation = project.currentPackage().getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Iterator<Diagnostic> iterator = diagnosticResult.errors().iterator();
        Diagnostic unusedModuleDiag = iterator.next();
        Assert.assertEquals(unusedModuleDiag.message(), "lambda functions are only allowed in ballerina projects");
    }
}
