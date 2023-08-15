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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Contains Utility methods required to generate handler functions.
 *
 * @since 2.0.0
 */
public class LambdaUtils {

    public static Diagnostic getDiagnostic(Location location, String code,
                                           String message, DiagnosticSeverity severity, Object... args) {

        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(code, message, severity);
        return DiagnosticFactory.createDiagnostic(diagnosticInfo, location, args);
    }

    public static boolean isAwsLambdaModule(ModuleID moduleId) {

        return moduleId.orgName().equals(Constants.LAMBDA_ORG_NAME) && moduleId.moduleName().equals(
                Constants.LAMBDA_MODULE_NAME);
    }

    public static boolean isAwsLambdaModule(ImportDeclarationNode importDeclarationNode) {

        if (importDeclarationNode.orgName().isEmpty()) {
            return false;
        }
        String orgName = importDeclarationNode.orgName().get().orgName().text();
        if (!Constants.LAMBDA_ORG_NAME.equals(orgName)) {
            return false;
        }

        SeparatedNodeList<IdentifierToken> moduleNames = importDeclarationNode.moduleName();
        if (moduleNames.size() != 2) {
            return false;
        }

        return Constants.AWS_ORG.equals(moduleNames.get(0).text()) &&
                Constants.LAMBDA_KEYWORD.equals(moduleNames.get(1).text());
    }

    public static FunctionDefinitionNode createMainFunction(SemanticModel semanticModel,
            Collection<FunctionDeploymentContext> functionDeploymentContexts) {

        FunctionDefinitionNode mainFunction = LambdaUtils.createMainFunction();
        for (FunctionDeploymentContext functionContext : functionDeploymentContexts) {
            String functionHandlerName = functionContext.getGeneratedFunction().functionName().text();
            PositionalArgumentNode handler = NodeFactory.createPositionalArgumentNode(
                    NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(functionHandlerName)));
            PositionalArgumentNode functionName =
                    NodeFactory.createPositionalArgumentNode(
                            LambdaUtils
                                    .createStringLiteral(
                                            functionContext.getOriginalFunction().functionName().text()));
            TypeDescriptorNode eventTypeDesc = getEventType(functionContext.getOriginalFunction(), semanticModel);
            PositionalArgumentNode typeDesc = NodeFactory.createPositionalArgumentNode(eventTypeDesc);
            ExpressionNode register =
                    createLambdaFunctionInvocationNode(Constants.LAMBDA_REG_FUNCTION_NAME, functionName, handler,
                            typeDesc);
            ExpressionStatementNode expressionStatementNode =
                    NodeFactory.createExpressionStatementNode(SyntaxKind.CALL_STATEMENT, register,
                            NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
            mainFunction = addStatementToFunctionBody(expressionStatementNode, mainFunction);
        }
        ExpressionNode processExpr = createLambdaFunctionInvocationNode("__process");
        ExpressionStatementNode processStatement =
                NodeFactory.createExpressionStatementNode(SyntaxKind.CALL_STATEMENT, processExpr,
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        return addStatementToFunctionBody(processStatement, mainFunction);
    }

    private static TypeDescriptorNode getEventType(FunctionDefinitionNode functionDefinitionNode,
                                                   SemanticModel semanticModel) {

        RequiredParameterNode requiredParameterNode =
                (RequiredParameterNode) functionDefinitionNode.functionSignature().parameters().get(1);
        IdentifierToken lambdaIdentifier = NodeFactory.createIdentifierToken(Constants.LAMBDA_KEYWORD);
        return (TypeDescriptorNode) getTypeNameNode(semanticModel, lambdaIdentifier, requiredParameterNode);
    }

    public static ModulePartNode createModulePartNode(Collection<FunctionDeploymentContext> functionDeploymentContexts,
                                                      FunctionDefinitionNode mainFunction) {

        ImportDeclarationNode afImport = NodeFactory.createImportDeclarationNode(NodeFactory
                        .createToken(SyntaxKind.IMPORT_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                LambdaUtils.generateMinutiaeListWithWhitespace()),
                NodeFactory.createImportOrgNameNode(NodeFactory.createIdentifierToken(Constants.LAMBDA_ORG_NAME),
                        NodeFactory.createToken(SyntaxKind.SLASH_TOKEN)),
                NodeFactory.createSeparatedNodeList(NodeFactory.createIdentifierToken(Constants.LAMBDA_MODULE_NAME)),
                null, NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                        LambdaUtils.generateMinutiaeListWithNewline()));

        List<ModuleMemberDeclarationNode> memberDeclarationNodeList = new ArrayList<>();

        memberDeclarationNodeList.add(mainFunction);

        for (FunctionDeploymentContext functionDeploymentContext : functionDeploymentContexts) {
            memberDeclarationNodeList.add(functionDeploymentContext.getGeneratedFunction());
        }
        NodeList<ModuleMemberDeclarationNode> nodeList = NodeFactory.createNodeList(memberDeclarationNodeList);
        Token eofToken = NodeFactory.createToken(SyntaxKind.EOF_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                LambdaUtils.generateMinutiaeListWithNewline());
        return NodeFactory.createModulePartNode(NodeFactory.createNodeList(afImport), nodeList, eofToken);
    }

    public static FunctionDefinitionNode createHandlerFunction(FunctionDefinitionNode originalFunctionDefNode,
                                                               SemanticModel semanticModel) {

        String originalFunctionName = originalFunctionDefNode.functionName().text();
        IdentifierToken lambdaIdentifier = NodeFactory.createIdentifierToken(Constants.LAMBDA_KEYWORD);
        QualifiedNameReferenceNode awsHandlerParamsType =
                NodeFactory.createQualifiedNameReferenceNode(
                        lambdaIdentifier,
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken(Constants.LAMBDA_CONTEXT,
                                NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()));
        RequiredParameterNode requiredParameterNode =
                NodeFactory.createRequiredParameterNode(NodeFactory.createEmptyNodeList(), awsHandlerParamsType,
                        NodeFactory.createIdentifierToken(Constants.CTX_PARAMS_NAME));

        BuiltinSimpleNameReferenceNode anydataType =
                NodeFactory.createBuiltinSimpleNameReferenceNode(SyntaxKind.ANYDATA_TYPE_DESC,
                        NodeFactory.createToken(SyntaxKind.ANYDATA_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        RequiredParameterNode inputParameterNode =
                NodeFactory.createRequiredParameterNode(NodeFactory.createEmptyNodeList(), anydataType,
                        NodeFactory.createIdentifierToken(Constants.INPUT_PARAMS_NAME));

        ReturnTypeDescriptorNode returnTypeDescriptorNode =
                originalFunctionDefNode.functionSignature().returnTypeDesc().orElse(null);

        FunctionSignatureNode functionSignatureNode =
                NodeFactory.createFunctionSignatureNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createSeparatedNodeList(requiredParameterNode,
                                NodeFactory.createToken(SyntaxKind.COMMA_TOKEN), inputParameterNode),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);

        PositionalArgumentNode ctxArg = NodeFactory.createPositionalArgumentNode(
                NodeFactory
                        .createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.CTX_PARAMS_NAME)));

        SimpleNameReferenceNode inputExpression = NodeFactory
                .createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(Constants.INPUT_PARAMS_NAME));
        RequiredParameterNode paramNode = (RequiredParameterNode) originalFunctionDefNode.functionSignature()
                .parameters().get(1);
        Node typeNameNode = getTypeNameNode(semanticModel, lambdaIdentifier, paramNode);
        TypeCastExpressionNode typeCastExpressionNode =
                NodeFactory.createTypeCastExpressionNode(NodeFactory.createToken(SyntaxKind.LT_TOKEN),
                        NodeFactory.createTypeCastParamNode(NodeFactory.createEmptyNodeList(), typeNameNode),
                        NodeFactory.createToken(SyntaxKind.GT_TOKEN), inputExpression);

        ReturnStatementNode returnStatementNode =
                NodeFactory.createReturnStatementNode(
                        NodeFactory.createToken(SyntaxKind.RETURN_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()),
                        createFunctionInvocationNode(originalFunctionName, ctxArg,
                                NodeFactory.createPositionalArgumentNode(typeCastExpressionNode)),
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        FunctionBodyBlockNode emptyFunctionBodyNode =
                NodeFactory.createFunctionBodyBlockNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                LambdaUtils.generateMinutiaeListWithNewline()), null,
                        NodeFactory.createNodeList(returnStatementNode),
                        NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN),
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));
        return NodeFactory.createFunctionDefinitionNode(
                SyntaxKind.FUNCTION_DEFINITION, null,
                NodeFactory.createNodeList(NodeFactory.createToken(SyntaxKind.PUBLIC_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace())),
                NodeFactory.createToken(SyntaxKind.FUNCTION_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()),
                NodeFactory.createIdentifierToken(Constants.PROXY_FUNCTION_PREFIX + originalFunctionName,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()),
                NodeFactory.createEmptyNodeList(), functionSignatureNode, emptyFunctionBodyNode);
    }

    private static Node getTypeNameNode(SemanticModel semanticModel, IdentifierToken lambdaIdentifier,
                                        RequiredParameterNode paramNode) {

        Node typeNameNode = paramNode.typeName();
        Optional<Symbol> typeSymbol = semanticModel.symbol(typeNameNode);
        if (typeSymbol.isPresent() && typeSymbol.get().getModule().isPresent() &&
                isAwsLambdaModule(typeSymbol.get().getModule().get().id()) &&
                typeNameNode.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            typeNameNode = ((QualifiedNameReferenceNode) typeNameNode).modify().withModulePrefix(lambdaIdentifier)
                    .apply();
        }
        return typeNameNode;
    }

    public static FunctionDefinitionNode createMainFunction() {

        OptionalTypeDescriptorNode optionalErrorTypeDescriptorNode =
                NodeFactory.createOptionalTypeDescriptorNode(
                        NodeFactory.createParameterizedTypeDescriptorNode(SyntaxKind.ERROR_TYPE_DESC,
                                NodeFactory.createToken(SyntaxKind.ERROR_KEYWORD), null),
                        NodeFactory.createToken(SyntaxKind.QUESTION_MARK_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                generateMinutiaeListWithWhitespace()));

        ReturnTypeDescriptorNode returnTypeDescriptorNode =
                NodeFactory.createReturnTypeDescriptorNode(NodeFactory
                                .createToken(SyntaxKind.RETURNS_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                                        generateMinutiaeListWithWhitespace()),
                        NodeFactory.createEmptyNodeList(), optionalErrorTypeDescriptorNode);
        FunctionSignatureNode functionSignatureNode =
                NodeFactory.createFunctionSignatureNode(NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                        NodeFactory.createSeparatedNodeList(),
                        NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);

        FunctionBodyBlockNode emptyFunctionBodyNode =
                NodeFactory.createFunctionBodyBlockNode(
                        NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN, NodeFactory.createEmptyMinutiaeList(),
                                LambdaUtils.generateMinutiaeListWithNewline()), null,
                        NodeFactory.createEmptyNodeList(), NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN),
                        NodeFactory.createToken(SyntaxKind.SEMICOLON_TOKEN));

        return NodeFactory.createFunctionDefinitionNode(
                SyntaxKind.FUNCTION_DEFINITION, null,
                NodeFactory.createNodeList(NodeFactory.createToken(SyntaxKind.PUBLIC_KEYWORD,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace())),
                NodeFactory.createToken(SyntaxKind.FUNCTION_KEYWORD, NodeFactory.createEmptyMinutiaeList(),
                        generateMinutiaeListWithWhitespace()),
                NodeFactory.createIdentifierToken(Constants.MAIN_FUNC_NAME,
                        NodeFactory.createEmptyMinutiaeList(), generateMinutiaeListWithWhitespace()),
                NodeFactory.createEmptyNodeList(), functionSignatureNode, emptyFunctionBodyNode);
    }

    public static MinutiaeList generateMinutiaeListWithWhitespace() {

        return NodeFactory.createMinutiaeList(NodeFactory.createWhitespaceMinutiae(" "));
    }

    public static MinutiaeList generateMinutiaeListWithNewline() {

        return NodeFactory.createMinutiaeList(NodeFactory.createWhitespaceMinutiae("\n"));
    }

    public static BasicLiteralNode createStringLiteral(String content) {

        return NodeFactory
                .createBasicLiteralNode(SyntaxKind.STRING_LITERAL, NodeFactory
                        .createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"" + content + "\"",
                                NodeFactory.createEmptyMinutiaeList(),
                                NodeFactory.createEmptyMinutiaeList()));
    }

    private static FunctionDefinitionNode addStatementToFunctionBody(StatementNode statementNode,
                                                                     FunctionDefinitionNode function) {

        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) function.functionBody();
        NodeList<StatementNode> newBodyStatements = functionBodyBlockNode.statements().add(statementNode);
        FunctionBodyBlockNode newFunctionBodyBlock =
                functionBodyBlockNode.modify().withStatements(newBodyStatements).apply();
        return function.modify().withFunctionBody(newFunctionBodyBlock).apply();
    }

    public static ExpressionNode createLambdaFunctionInvocationNode(String functionName,
                                                                    PositionalArgumentNode... args) {

        QualifiedNameReferenceNode qualifiedNameReferenceNode =
                NodeFactory.createQualifiedNameReferenceNode(
                        NodeFactory.createIdentifierToken(Constants.LAMBDA_KEYWORD),
                        NodeFactory.createToken(SyntaxKind.COLON_TOKEN),
                        NodeFactory.createIdentifierToken(functionName));
        SeparatedNodeList<FunctionArgumentNode> separatedNodeList = getFunctionParamList(args);

        return NodeFactory.createFunctionCallExpressionNode(qualifiedNameReferenceNode,
                NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN), separatedNodeList,
                NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));
    }

    private static SeparatedNodeList<FunctionArgumentNode> getFunctionParamList(PositionalArgumentNode... args) {

        List<Node> nodeList = new ArrayList<>();
        for (PositionalArgumentNode arg : args) {
            nodeList.add(arg);
            nodeList.add(NodeFactory.createToken(SyntaxKind.COMMA_TOKEN));
        }
        if (args.length > 0) {
            nodeList.remove(nodeList.size() - 1);
        }
        return NodeFactory.createSeparatedNodeList(nodeList);
    }

    public static ExpressionNode createFunctionInvocationNode(String functionName, PositionalArgumentNode... args) {

        SimpleNameReferenceNode simpleNameReferenceNode =
                NodeFactory.createSimpleNameReferenceNode(NodeFactory.createIdentifierToken(functionName));
        SeparatedNodeList<FunctionArgumentNode> separatedNodeList = getFunctionParamList(args);
        return NodeFactory.createFunctionCallExpressionNode(simpleNameReferenceNode,
                NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN), separatedNodeList,
                NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));
    }

    public static Path getFunctionsDir(Project project, Path jarPath) {

        return getTargetDir(project, jarPath).resolve(Constants.FUNCTION_DIRECTORY);
    }

    public static Path getTargetDir(Project project, Path jarPath) {

        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return jarPath.getParent();
        }
        return project.targetDir();
    }

    public static void deleteDirectory(Path azureFunctionsDir) throws IOException {

        if (azureFunctionsDir.toFile().exists()) {
            Files.walk(azureFunctionsDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
