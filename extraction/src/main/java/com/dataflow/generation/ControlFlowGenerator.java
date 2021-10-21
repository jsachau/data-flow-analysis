package com.dataflow.generation;

import com.dataflow.generation.controlflow.ControlFlow;
import com.dataflow.generation.controlflow.NodeHandler;
import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.JavaParserBuild;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import static com.github.javaparser.ast.Node.SYMBOL_RESOLVER_KEY;


public class ControlFlowGenerator extends JavaParserComponent {

    /**
     * Generates a control flow.
     *
     * @param inputClass Path to the mapping class.
     * @return control flow
     */
    public static ControlFlow generate(String inputClass, SimpleName methodName) throws Exception {
        CompilationUnit cu = createCompilationUnit(inputClass);

        Optional<MethodDeclaration> methodDeclaration =
                cu.findAll(MethodDeclaration.class).stream().filter(md -> md.getName().equals(methodName)).findFirst();

        if (!methodDeclaration.isPresent()) {
            throw new Exception("Method " + methodName + " not found");
        }

        ControlFlow controlFlow = initialize(methodDeclaration.get());
        extractCompositionNameAndFhirProfileType(cu, controlFlow);

        return controlFlow;
    }

    public static ControlFlow generate(MethodDeclaration methodDeclaration) throws Exception {

        methodDeclaration.findCompilationUnit().get().setData(SYMBOL_RESOLVER_KEY, getSymbolResolver());

        if(!methodDeclaration.resolve().toAst().isPresent()) {
            throw new Exception("Method not resolvable");
        }

        ControlFlow controlFlow = initialize(methodDeclaration);
        //extractCompositionNameAndFhirProfileType(controlFlow);
        return controlFlow;
    }

    private static ControlFlow initialize(MethodDeclaration methodDeclaration) throws IOException {
        ControlFlow controlFlow = new ControlFlow(methodDeclaration);
        NodeHandler nodeHandler = new NodeHandler(controlFlow);

        nodeHandler.handleNode(methodDeclaration, null);

        return controlFlow;
    }

    private static void extractCompositionNameAndFhirProfileType(CompilationUnit cu, ControlFlow controlFlow) {
        if (!cu.getTypes().get(0).toClassOrInterfaceDeclaration().isPresent() ||
                !cu.getTypes().get(0).toClassOrInterfaceDeclaration().get().getImplementedTypes().get(0).toClassOrInterfaceType().isPresent() &&
                        !cu.getTypes().get(0).toClassOrInterfaceDeclaration().get().getImplementedTypes().get(0).toClassOrInterfaceType().get().getTokenRange().isPresent()) {
            return;
        }

        Iterator<JavaToken> it = cu.getTypes().get(0).toClassOrInterfaceDeclaration().get().getImplementedTypes().
                get(0).toClassOrInterfaceType().get().getTokenRange().get().iterator();

        boolean foundStartArgument = false;
        while (it.hasNext()) {
            JavaToken token = it.next();
            String tokenString = token.asString();

            if (!token.asString().trim().equals(",") && !token.asString().trim().isEmpty() && foundStartArgument) {
                if (controlFlow.compositionName == null) {
                    controlFlow.compositionName = new SimpleName(token.asString());
                }
            }

            if (token.asString().equals("<")) {
                foundStartArgument = true;
            }
        }
    }

    private static boolean isCompositionType(String qualifiedName) {
        return qualifiedName.startsWith("org.ehrbase.fhirbridge.ehr.opt.");
    }

    private static void extractCompositionNameAndFhirProfileType(ControlFlow controlFlow) {
        if (!controlFlow.method.getTypeAsString().equals("void")) {
            controlFlow.compositionName = new SimpleName(controlFlow.method.getTypeAsString());
        }

        for (Parameter param : controlFlow.method.getParameters()) {

            String qualifiedName = "";

            try {
                qualifiedName = param.resolve().getType().asReferenceType().getQualifiedName();
            } catch (IllegalStateException ex) {
            }

            if (!qualifiedName.isEmpty() && isCompositionType(qualifiedName)) {
                controlFlow.compositionName = new SimpleName(param.getTypeAsString());
            }
        }

    }
}
