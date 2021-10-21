package com.dataflow.generation;

import com.dataflow.generation.controlflow.ControlFlow;
import com.dataflow.generation.controlflow.NodeHandler;
import com.github.javaparser.JavaToken;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

public class MappingExtractor extends JavaParserComponent {

    private static String SETTER_METHOD = "afterPropertiesSet";

    private static List<String> IGNORE_MAPPINGS = new ArrayList<String>() {{

    }};

    public static class MappingRequest {
        public String name;
        public String mappingClassPath;

        MappingRequest() {}

        MappingRequest(String name, String mappingClassPath, String compositionDir) {
            this.name = name;
            this.mappingClassPath = mappingClassPath;
        }
    }

    /**
     * Generates the mapping requests
     *
     * @return mapping requests
     */
    public List<MappingRequest> generateRequests() {
        CompilationUnit cu = createCompilationUnit(Configuration.FHIR_BRIDGE_BASE_DIR + "org/ehrbase/fhirbridge/ehr/converter/" + "CompositionConverterResolver.java");

        List<MappingRequest> requests = new ArrayList<>();
        createMethod(cu, requests);

        return requests;
    }



    private void createMethod(CompilationUnit cu, List<MappingRequest> requests ) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            List<Node> childNodes = type.getChildNodes();
            for (Node node : childNodes) {
                if (node instanceof MethodDeclaration) {
                    MethodDeclaration md = (MethodDeclaration) node;

                    if(md.getName().asString().equals(SETTER_METHOD) && md.getBody().isPresent()) {
                        for (Node childNode: md.getBody().get().getChildNodes()){

                            ExpressionStmt expressionStmt = (ExpressionStmt) childNode;

                            if(expressionStmt.getExpression().toMethodCallExpr().isPresent())
                            {
                                MappingRequest mr = new MappingRequest();
                                for(Expression expr : expressionStmt.getExpression().toMethodCallExpr().get().getArguments())
                                {
                                    if(expr.toFieldAccessExpr().isPresent())
                                    {
                                        mr.name = expr.toFieldAccessExpr().get().toString().substring(8);
                                    }

                                    if(expr.toObjectCreationExpr().isPresent())
                                    {
                                        String converterName = expr.toObjectCreationExpr().get().getType().resolve().getQualifiedName();

                                        mr.mappingClassPath = Configuration.FHIR_BRIDGE_BASE_DIR + converterName.replace(".", "/") + ".java";
                                    }
                                }
                                //mr.name.equals("SYMPTOMS_COVID_19") &&
                                //mr.name.equals("SOFA_SCORE") &&
                                //mr.name.equals("SMOKING_STATUS") &&
                                //mr.name.equals("DIAGNOSTIC_REPORT_LAB")
                                if(!mr.name.equals("CORONARIRUS_NACHWEIS_TEST") && !IGNORE_MAPPINGS.contains(mr.name))
                                {
                                    requests.add(mr);
                                } else {
                                    System.out.println("Skipping " + mr.name);
                                }
                            }


                        }
                    }
                }
            }
        }

    }



}
