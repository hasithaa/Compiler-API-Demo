/*
 * MIT License
 *
 * Copyright (c) 2023 Hasitha Aravinda. All rights reserved.
 *
 * This work is licensed under the terms of the MIT license.
 * For a copy, see <https://opensource.org/licenses/MIT>.
 */
package tips.bal.examples.compiler;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.impl.symbols.BallerinaConstantSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MINUS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEGATION_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NUMERIC_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SPECIFIC_FIELD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.UNARY_EXPRESSION;

/**
 * Basic Annotation Validator using Compiler APIs.
 *
 * @since 1.0.0
 */
public class AnnotationAnalyser {

    private static final String PROJECT_PATH = "proj1";
    private static final String EXAMPLE_HOME = "ballerina-sources";
    private static final String USER_DIR = "user.dir";


    public List<String> analyseStudentAnnotations() {
        final Project project = getAnnotationProject();
        final Package currentPackage = project.currentPackage();
        final PackageCompilation compilation = currentPackage.getCompilation();// Compile

        if (compilation.diagnosticResult().diagnosticCount() > 0) {
            throw new IllegalStateException("Compilation failed");
        }

        final Module defaultModule = currentPackage.getDefaultModule();

        // Get the syntax tree of the document. (Usually we get the document from compiler tools)
        final Stream<Document> documentStream = defaultModule.documentIds().stream().map(defaultModule::document);
        final Document mainbal = documentStream.filter(document -> document.name().contains("main.bal"))
                .findFirst().orElseThrow();

        final SyntaxTree syntaxTree = mainbal.syntaxTree();

        // Get the semantic model
        final SemanticModel semanticModel = compilation.getSemanticModel(defaultModule.moduleId());

        return processStudentAnnotations(syntaxTree, semanticModel);
    }

    public List<String> processStudentAnnotations(SyntaxTree syntaxTree, SemanticModel semModel) {
        final TypeDefVisitor typeDefVisitor = new TypeDefVisitor();
        syntaxTree.rootNode().accept(typeDefVisitor);
        final List<TypeDefinitionNode> typeDefinitionNodes = typeDefVisitor.getTypeDefinitionNodes();
        final Node studentType = typeDefinitionNodes.stream()
                .filter(td -> td.typeName().text().equals("Student"))
                .findFirst()
                .orElseThrow().typeDescriptor();

        if (studentType.kind() != RECORD_TYPE_DESC) {
            throw new IllegalStateException("Student type is not a record");
        }
        RecordTypeDescriptorNode recordNode = (RecordTypeDescriptorNode) studentType;
        List<String> results = new ArrayList<>();
        for (Node node : recordNode.fields()) {
            RecordFieldNode fieldNode = (RecordFieldNode) node;
            if (fieldNode.metadata().isPresent()) {
                final MetadataNode metadataNode = fieldNode.metadata().get();
                for (AnnotationNode annotation : metadataNode.annotations()) {
                    final String annotationName = annotation.annotReference().toString();
                    if (annotation.annotValue().isPresent()) {
                        SeparatedNodeList<MappingFieldNode> annotationFields = annotation.annotValue().get().fields();
                        for (MappingFieldNode mappingFieldNode : annotationFields) {
                            if (mappingFieldNode.kind() == SPECIFIC_FIELD) {
                                final SpecificFieldNode specificFieldNode = (SpecificFieldNode) mappingFieldNode;
                                final String fieldName = specificFieldNode.fieldName().toString().trim();

                                String valueType = semModel.symbol(specificFieldNode)
                                        .map(f -> ((RecordFieldSymbol) f).typeDescriptor().signature())
                                        .orElse(null);
                                String value = null;

                                // Must have a value, otherwise compilation error.
                                final ExpressionNode expressionNode = specificFieldNode.valueExpr().orElse(null);
                                Symbol symbol = semModel.symbol(expressionNode).orElse(null);

                                // First check if the value is a constant, then only check for literals.
                                if (symbol != null && symbol.kind() == SymbolKind.CONSTANT) {
                                    final BallerinaConstantSymbol constantSymbol = (BallerinaConstantSymbol) symbol;
                                    value = constantSymbol.constValue().toString();
                                    // Correct type from Value
                                    valueType = constantSymbol.typeDescriptor().signature();
                                } else if (expressionNode != null) {
                                    if (expressionNode.kind() == NUMERIC_LITERAL
                                            || expressionNode.kind() == STRING_LITERAL
                                            || expressionNode.kind() == BOOLEAN_LITERAL) {
                                        Token literalToken = ((BasicLiteralNode) expressionNode).literalToken();
                                        value = literalToken.text();
                                    } else if (expressionNode.kind() == UNARY_EXPRESSION) {
                                        UnaryExpressionNode unaryExpressionNode = (UnaryExpressionNode) expressionNode;
                                        if (unaryExpressionNode.unaryOperator().kind() == MINUS_TOKEN) {
                                            Token literalToken = ((BasicLiteralNode) unaryExpressionNode.expression())
                                                    .literalToken();
                                            value = "-" + literalToken.text();
                                        }
                                    }
                                }
                                results.add("Annotation " + annotationName + "field: " + fieldName +
                                        " value: " + value + " type: " + valueType);
                            }
                        }
                    }
                }
            }

        }
        return results;
    }

    private Project getAnnotationProject() {
        final CompilerRunner compilerRunner = new CompilerRunner();
        final Path userDir = Path.of(System.getProperty(USER_DIR));
        return compilerRunner.compile(Paths.get(userDir.toString(), EXAMPLE_HOME, PROJECT_PATH));
    }

    /**
     * Visitor to get the TypeDefinitionNode.
     */
    private static class TypeDefVisitor extends NodeVisitor {

        List<TypeDefinitionNode> typeDefinitionNodes = new ArrayList<>();

        @Override
        public void visit(TypeDefinitionNode typeDefinitionNode) {
            typeDefinitionNodes.add(typeDefinitionNode);
        }

        public List<TypeDefinitionNode> getTypeDefinitionNodes() {
            return typeDefinitionNodes;
        }
    }
}
