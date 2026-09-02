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
package org.ballerinax.aws.lambda.generator;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<FunctionDefinitionNode, LambdaFunctionInfo.Destinations> destinations;

    public LambdaFunctionVisitor(SemanticModel semanticModel) {
        this.functions = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.diagnostics = new ArrayList<>();
        // Keyed by node identity rather than by name: the codegen task looks these up against a
        // process-wide list of generated functions that accumulates across invocations, where two
        // packages could each contribute a function of the same name.
        this.destinations = new IdentityHashMap<>();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
        if (symbol.isEmpty() || !(symbol.get() instanceof FunctionSymbol)) {
            // Every function in the document reaches this point, so only an annotated one is worth
            // reporting. Without the symbol the annotation can only be read off the syntax tree,
            // and a function that looks annotated but resolves to nothing would otherwise be
            // dropped from the generated list with the build still reporting success.
            if (hasLambdaFunctionAnnotation(functionDefinitionNode)) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionDefinitionNode.location(), "AZ0005",
                        "Unable to resolve the symbol of this AWS lambda function, so it is not deployed",
                        DiagnosticSeverity.WARNING));
            }
            return;
        }
        FunctionSymbol functionSymbol = (FunctionSymbol) symbol.get();
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
            List<ParameterSymbol> parameters = functionTypeSymbol.params().orElse(Collections.emptyList());
            if (parameters.size() != 2) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionDefinitionNode.location(), "AZ0001",
                        "Invalid function signature for an AWS lambda function, it should be 'public " +
                                "function (lambda:Context, anydata) returns json|error'",
                        DiagnosticSeverity.ERROR));
                continue;
            }
            ParameterSymbol contextParam = parameters.get(0);
            ParameterSymbol secondParam = parameters.get(1);

            // A function whose parameters are wrong cannot be generated, so it is reported and
            // skipped rather than handed on to code generation, which does not see diagnostics.
            boolean invalidParameters = false;
            if (contextParam.getName().isEmpty() || secondParam.getName().isEmpty()) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionDefinitionNode.location(), "AZ0003",
                        "AWS lambda does not support empty params", DiagnosticSeverity.ERROR));
                invalidParameters = true;
            }
            if (contextParam.paramKind() != ParameterKind.REQUIRED
                    || secondParam.paramKind() != ParameterKind.REQUIRED) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionDefinitionNode.location(), "AZ0002",
                        "AWS lambda only supports required parameters", DiagnosticSeverity.ERROR));
                invalidParameters = true;
            }
            if (invalidParameters) {
                continue;
            }

            if (!isContext(contextParam.typeDescriptor())) {
                this.diagnostics.add(LambdaUtils.getDiagnostic(functionDefinitionNode.location(), "AZ0004",
                        "First parameter of AWS Lambda function should be `lambda:Context`",
                        DiagnosticSeverity.ERROR));
                continue;
            }

            Optional<TypeSymbol> returnTypeDescriptor = functionSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDescriptor.isEmpty()) {
                this.functions.add(functionDefinitionNode);
                this.recordDestinations(functionDefinitionNode, functionSymbol);
            } else {
                if (isValidReturnType(returnTypeDescriptor.get())) {
                    this.functions.add(functionDefinitionNode);
                    this.recordDestinations(functionDefinitionNode, functionSymbol);
                } else {
                    // The generated handler reuses this return type verbatim and has to satisfy
                    // the runtime's 'function (Context, anydata) returns json|error', so a record
                    // such as FunctionURLResponse is rejected here rather than in generated code,
                    // where the failure would point at a file the user did not write.
                    String returnType = returnTypeDescriptor.get().signature();
                    this.diagnostics.add(LambdaUtils.getDiagnostic(functionDefinitionNode.location(), "AZ0004",
                            returnType + " is not a supported return type for AWS functions. Return " +
                                    "'json' and convert the value with '.toJson()'",
                            DiagnosticSeverity.ERROR));
                }
            }
        }

    }

    /**
     * Reads the destinations given on the {@code @lambda:Function} annotation, if any. The
     * annotation type has only optional fields, so an attachment with no value yields an empty
     * map and nothing is recorded.
     */
    private void recordDestinations(FunctionDefinitionNode functionDefinitionNode, FunctionSymbol functionSymbol) {
        for (AnnotationAttachmentSymbol attachment : functionSymbol.annotAttachments()) {
            Optional<ModuleSymbol> module = attachment.typeDescriptor().getModule();
            if (module.isEmpty() || !LambdaUtils.isAwsLambdaModule(module.get().id())) {
                continue;
            }
            Optional<ConstantValue> attachmentValue = attachment.attachmentValue();
            if (attachmentValue.isEmpty()) {
                continue;
            }
            Object destinations = readField(unwrap(attachmentValue.get().value()), "destinations");
            if (destinations == null) {
                continue;
            }
            String onSuccess = asString(readField(destinations, "onSuccess"));
            String onFailure = asString(readField(destinations, "onFailure"));
            LambdaFunctionInfo.Destinations value = new LambdaFunctionInfo.Destinations(onSuccess, onFailure);
            if (!value.isEmpty()) {
                this.destinations.put(functionDefinitionNode, value);
            }
        }
    }

    /**
     * The compiler API wraps annotation values in {@code ConstantValue}, and nests them for record
     * fields, so values are unwrapped defensively rather than cast.
     */
    private static Object unwrap(Object value) {
        if (value instanceof ConstantValue) {
            return unwrap(((ConstantValue) value).value());
        }
        return value;
    }

    private static Object readField(Object mapping, String field) {
        if (!(mapping instanceof Map)) {
            return null;
        }
        return unwrap(((Map<?, ?>) mapping).get(field));
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Reports whether the function carries something that reads as an {@code @lambda:Function}
     * annotation, going by the syntax tree alone. Used only where the symbol could not be resolved
     * and the annotation therefore cannot be checked properly, so it is deliberately loose: the
     * prefix is whatever the document imported the module as, and a false positive here reports a
     * function that was already failing to resolve.
     *
     * @param functionDefinitionNode the function to check
     * @return whether the function looks like an AWS lambda function
     */
    private static boolean hasLambdaFunctionAnnotation(FunctionDefinitionNode functionDefinitionNode) {
        Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
        if (metadata.isEmpty()) {
            return false;
        }
        for (AnnotationNode annotation : metadata.get().annotations()) {
            Node reference = annotation.annotReference();
            if (reference.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && ((QualifiedNameReferenceNode) reference).identifier().text().equals("Function")) {
                return true;
            }
        }
        return false;
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
        Optional<String> name = typeSymbol.getName();
        if (name.isPresent() && name.get().equals("Context")) {
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

    public List<Diagnostic> getDiagnostics() {
        return this.diagnostics;
    }

    /**
     * Destinations by function name, for the functions that declared any.
     *
     * @return the declared destinations
     */
    public Map<FunctionDefinitionNode, LambdaFunctionInfo.Destinations> getDestinations() {
        return this.destinations;
    }
}
