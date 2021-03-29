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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Visitor for Ballerina Document to extract and validate AWS Functions.
 *
 * @since 2.0.0
 */
public class LambdaFunctionVisitor extends NodeVisitor {

    private final List<FunctionDefinitionNode> functions;
    private final SemanticModel semanticModel;
    private final List<Diagnostic> diagnostics;

    public LambdaFunctionVisitor(SemanticModel semanticModel) {
        this.functions = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.diagnostics = new ArrayList<>();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        FunctionSymbol functionSymbol = (FunctionSymbol) semanticModel.symbol(functionDefinitionNode).get();
        List<AnnotationSymbol> annotations = functionSymbol.annotations();
        for (AnnotationSymbol annotationSymbol : annotations) {
            if (annotationSymbol.getModule().isEmpty()) {
                continue;
            }
            ModuleID moduleId = annotationSymbol.getModule().get().id();
            if (!LambdaUtils.isAwsLambdaModule(moduleId)) {
                continue;
            }
            if (annotationSymbol.getName().isEmpty() || !annotationSymbol.getName().get().equals("Function")) {
                continue;
            }
            FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
            List<ParameterSymbol> parameters = functionTypeSymbol.params().get();
            if (parameters.size() != 2) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionTypeSymbol.location(), "AZ0001",
                        "Invalid function signature for an AWS lambda function: , it should be 'public " +
                                "function (awslambda:Context, anydata) returns json|error'",
                        DiagnosticSeverity.ERROR));
            }
            ParameterSymbol contextParam = parameters.get(0);
            ParameterSymbol secondParam = parameters.get(1);

            if (contextParam.getName().isEmpty()) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(contextParam.location(), "AZ0003",
                        "AWS lambda does not support empty params", DiagnosticSeverity.ERROR));
            }
            if (contextParam.paramKind() != ParameterKind.REQUIRED) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(contextParam.location(), "AZ0002",
                        "AWS lambda only supports required parameters", DiagnosticSeverity.ERROR));

            }

            if (secondParam.getName().isEmpty()) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(contextParam.location(), "AZ0003",
                        "AWS lambda does not support empty params", DiagnosticSeverity.ERROR));
            }
            if (secondParam.paramKind() != ParameterKind.REQUIRED) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(contextParam.location(), "AZ0003",
                        "AWS lambda does not support empty params", DiagnosticSeverity.ERROR));
            }

            if (!isContext(contextParam.typeDescriptor())) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionTypeSymbol.location(), "AZ0004",
                        "First parameter of AWS Lambda function should be `awslambda:Context`",
                        DiagnosticSeverity.ERROR));

            }

            Optional<TypeSymbol> returnTypeDescriptor = functionSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDescriptor.isEmpty()) {
                this.functions.add(functionDefinitionNode);
            } else {
                if (isValidReturnType(returnTypeDescriptor.get())) {
                    this.functions.add(functionDefinitionNode);
                } else {
                    this.diagnostics.add(LambdaUtils.getDiagnostic(functionTypeSymbol.location(), "AZ0004",
                            returnTypeDescriptor.get().signature() + "is not a supported return type for AWS functions",
                            DiagnosticSeverity.ERROR));
                }
            }
        }

    }

    private boolean isValidReturnType(TypeSymbol typeSymbol) {
        switch (typeSymbol.typeKind()) {
            case JSON:
            case ERROR:
            case NIL:
                return true;
            case UNION:
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                Set<TypeDescKind> typeTags = new HashSet<>();
                for (TypeSymbol memberTypeDescriptor : unionTypeSymbol.memberTypeDescriptors()) {
                    typeTags.add(memberTypeDescriptor.typeKind());
                }
                typeTags.remove(TypeDescKind.JSON);
                typeTags.remove(TypeDescKind.ERROR);
                typeTags.remove(TypeDescKind.NIL);
                return typeTags.isEmpty();
            default:
                return false;
        }
    }

    private boolean isContext(TypeSymbol typeSymbol) {
        if (typeSymbol.getName().get().equals("Context")) {
            Optional<ModuleSymbol> module = typeSymbol.getModule();
            if (module.isEmpty()) {
                return false;
            }
            return LambdaUtils.isAwsLambdaModule(module.get().id());
        }
        return false;
    }

    public List<FunctionDefinitionNode> getFunctions() {
        return this.functions;
    }
}
