package com.dataflow.analysis;

import com.dataflow.DataFlowAnalysis;
import com.dataflow.analysis.dependency.*;
import com.dataflow.analysis.utils.JavaParserUtils;
import com.dataflow.exportable.TemplateClass;
import com.dataflow.exportable.TemplateField;
import com.dataflow.generation.ControlFlowGenerator;
import com.dataflow.generation.controlflow.ControlFlow;
import com.dataflow.generation.controlflow.ControlFlowVertex;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DependencyGraphBuilder {

    private ControlFlow controlFlow;

    private List<TemplateClass> templateClasses;

    private DependencyGraph dependencyGraph;

    private Set<ReachingDefinitions.Assignment> assignments;
    private Set<SimpleName> resolvedVariables;

    private TemplateClass templateClass;

    public DependencyGraphBuilder(ControlFlow controlFlow,
                                  List<TemplateClass> templateClasses) {
        this.controlFlow = controlFlow;
        this.templateClasses = templateClasses;
        this.resolvedVariables = new HashSet<>();
    }

    public DependencyGraph build() throws Exception {

        Optional<ControlFlowVertex> startVertex = this.controlFlow.graph.vertexSet().stream().filter(v -> v.getNode().equals(this.controlFlow.method)).findFirst();

        if (!startVertex.isPresent()) {
            throw new Exception("Start vertex not present");
        }


        ReachingDefinitions reachingDefinitionsAnalysis =
                new ReachingDefinitions(this.controlFlow.graph);

        this.assignments = reachingDefinitionsAnalysis.performFixedPointAnalysis();

        //System.out.println("Assignments (" + this.controlFlow.method.getName() + "): " + variableAssignments);

        this.dependencyGraph = new MethodDependencyGraph(this.controlFlow.method);


        for (int i = 0; i < this.controlFlow.method.getParameters().size(); i++) {
            Parameter param = this.controlFlow.method.getParameters().get(i);
            // Adding a variable vertex as an "argument"
            this.dependencyGraph.addVertex(new VariableVertex(param.getName(), param.getType().resolve(), true, i));
        }


        this.resolveReturnType();
        this.resolveOtherMethods();
        this.resolveMethodAssignments();

        this.resolveAllVariables();
        //DependencyGraphUtils.printGraph(this.dependencyGraph);

        /*Optional<DependencyVertex> fhirProfileVertex =
                this.dependencyGraph.vertexSet().stream().filter(v ->
                        v instanceof VariableVertex &&
                                ((VariableVertex) v).getVariableName().asString().equals(this.fhirProfileName.asString())).findFirst();

        if (!fhirProfileVertex.isPresent()) {
            throw new Exception("FHIR Profile vertex not found");
        }

        ((VariableVertex) fhirProfileVertex.get()).setIsFHIRProfile(true);*/


        return this.dependencyGraph;
    }

    private void resolveMethodAssignments() throws Exception {

        for (ReachingDefinitions.Assignment assignment : this.assignments) {

            if (assignment.scope == null) continue;

            if (assignment.vertex.getNode() instanceof ExpressionStmt) {
                ExpressionStmt expressionStmt = (ExpressionStmt) assignment.vertex.getNode();

                if (expressionStmt.getExpression().toMethodCallExpr().isPresent()) {
                    if (assignment.scope.toMethodCallExpr().isPresent()) {
                        TemplateClass scopeType = this.getVariableType(assignment.scope.toMethodCallExpr().get().resolve().getReturnType());
                        if (scopeType != null && !scopeType.isEnumValueSet) {
                            for (TemplateField field : scopeType.fields) {
                                DependencyVertex fieldVertex = new TemplateFieldVertex(field);
                                this.dependencyGraph.addVertex(fieldVertex);
                                this.resolveSetterMethod(assignment.vertex, assignment.scope.toString(), fieldVertex);
                                //System.out.println("New assignment " + assignment.vertex.getNode() + " " + assignment.vertex.getNode().getClass().toString());
                            }
                        }
                    }
                }
            }
        }

    }

    private DependencyGraph buildDependencyGraphFromMethodDeclaration(MethodCallExpr methodCallExpr, MethodDeclaration methodDeclaration) throws Exception {
        ControlFlow cf = ControlFlowGenerator.generate(methodDeclaration);
        DependencyGraphBuilder dependencyGraphBuilder = new DependencyGraphBuilder(cf, this.templateClasses);

        DependencyGraph dg = dependencyGraphBuilder.build();

        this.dependencyGraph.addGraph(dg);

        List<VariableVertex> argumentVertices = dg.getArgumentVertices();

        if (methodCallExpr.getArguments().size() != argumentVertices.size()) {
            throw new Exception("Method does not have the same argument count as expected " + methodCallExpr + " has arguments" + argumentVertices);
        }

        for (int i = 0; i < argumentVertices.size(); i++) {
            addExpressionToDependencyGraph(methodCallExpr.getArgument(i), argumentVertices.get(i));
        }

        return dg;
    }

    private void resolveReturnType() throws Exception {
        Type methodType = this.controlFlow.method.getType();

        if (methodType.isVoidType()) {
            return;
        }

        ReturnVertex returnVertex = new ReturnVertex(methodType);

        for (ControlFlowVertex vertex : this.controlFlow.graph.vertexSet()) {
            if (vertex.getNode() instanceof ReturnStmt) {

                ReturnStmt returnStmt = (ReturnStmt) vertex.getNode();

                if (!returnStmt.getExpression().isPresent()) {
                    throw new Exception("Return statement has no expression but method returns a value " + returnStmt);
                }

                Expression expression = returnStmt.getExpression().get();
                this.dependencyGraph.addVertex(returnVertex);
                this.addExpressionToDependencyGraph(expression, returnVertex);
                this.resolveContainingIfStatements(vertex, returnVertex);
            }
        }

        if (!this.dependencyGraph.getReturnVertex().isPresent()) {
            DataFlowAnalysis.createAndShowGui(this.controlFlow.graph);
        }
    }


    private void resolveAllVariables() throws Exception {
        Optional<DependencyVertex> vertex;

        do {
            vertex = this.dependencyGraph.methodVertexSet().stream().filter(v -> v instanceof VariableVertex &&
                    !this.resolvedVariables.contains(((VariableVertex) v).getVariableName())).findFirst();

            //System.out.println("Resolving variable " + vertex);
            if (vertex.isPresent()) {
                VariableVertex variableVertex = ((VariableVertex) vertex.get());
                SimpleName variable = variableVertex.getVariableName();

                TemplateClass variableType = this.getVariableType((VariableVertex) vertex.get());

                if (variableType != null && !variableType.isEnumValueSet) {
                    for (TemplateField field : variableType.fields) {
                        this.resolveFields(variableVertex, field);
                    }
                }

                this.resolveDependencies(variableVertex);
                this.resolvedVariables.add(variable);
            }
        } while (vertex.isPresent());

    }

    private void resolveMethodCall(Expression expression, DependencyEdge edge) throws Exception {
        if (expression.toMethodCallExpr().isPresent()) {
            MethodCallExpr methodCallExpr = expression.toMethodCallExpr().get();

            if (!methodCallExpr.getScope().isPresent() || methodCallExpr.getScope().toString().equals("this")) {
                // Resolve new method method(exportable, observation);

                if (methodCallExpr.getArguments() != null) {
                    MethodCallVertex methodVertex = new MethodCallVertex(methodCallExpr);

                    this.dependencyGraph.addVertex(methodVertex);

                    if (methodVertex.isResolvableMethod()) {
                        DependencyGraph dg = this.buildDependencyGraphFromMethodDeclaration(methodCallExpr,
                                methodVertex.getMethodDeclaration());

                        if (dg.getReturnVertex().isPresent()) {
                            this.dependencyGraph.addEdge(methodVertex, dg.getReturnVertex().get(), edge);
                        }
                    }
                }

            } else {
                //System.out.println(expression);
            }
        }
    }

    private void resolveOtherMethods() throws Exception {

        for (ControlFlowVertex vertex : this.controlFlow.graph.vertexSet()) {
            Node node = vertex.getNode();

            if (node instanceof ExpressionStmt) {
                ExpressionStmt expressionStmt = (ExpressionStmt) node;
                Expression expression = expressionStmt.getExpression();
                resolveMethodCall(expression, new DependencyEdge());
            }
        }
    }

    private void resolveMethodCall(MethodCallExpr methodCallExpr, DependencyVertex source, DependencyEdge edge) throws Exception {


        MethodCallVertex vertex = new MethodCallVertex(methodCallExpr);
        dependencyGraph.addVertex(vertex);

        dependencyGraph.addEdge(source, vertex, edge);

        if (vertex.isResolvableMethod()) {
            DependencyGraph dg = this.buildDependencyGraphFromMethodDeclaration(methodCallExpr,
                    vertex.getMethodDeclaration());

            if (dg.getReturnVertex().isPresent()) {
                this.dependencyGraph.addEdge(vertex, dg.getReturnVertex().get());
            }
        } else {
            for (Expression expr : methodCallExpr.getArguments()) {
                addExpressionToDependencyGraph(expr, vertex);
            }
        }

        if (methodCallExpr.getScope().isPresent()) {
            addExpressionToDependencyGraph(methodCallExpr.getScope().get(), vertex);
        }

    }

    private void addExpressionToDependencyGraph(Expression expression, DependencyVertex source) throws Exception {
        addExpressionToDependencyGraph(expression, source, new DependencyEdge());
    }

    private void addExpressionToDependencyGraph(Expression expression, DependencyVertex source, DependencyEdge edge) throws Exception {

        DependencyVertex vertex = null;

        if (expression.toMethodCallExpr().isPresent()) {

            resolveMethodCall(expression.toMethodCallExpr().get(), source, edge);
            return;

        } else if (expression.toNameExpr().isPresent()) {
            try {
                vertex = new VariableVertex(expression.toNameExpr().get().getName(), expression.toNameExpr().get().resolve().getType());
            } catch (UnsolvedSymbolException ex) {
                System.out.println("Could not resolve " + expression.toNameExpr().get().getName() + " " + ex.getMessage());
            }

        } else if (expression.toCastExpr().isPresent()) {
            addExpressionToDependencyGraph(expression.toCastExpr().get().getExpression(), source, edge);
        } else if (expression.toEnclosedExpr().isPresent()) {
            addExpressionToDependencyGraph(expression.toEnclosedExpr().get().getInner(), source, edge);
        } else if (expression.toUnaryExpr().isPresent()) {
            addExpressionToDependencyGraph(expression.toUnaryExpr().get().getExpression(), source, edge);
        } else if (expression.toLiteralExpr().isPresent()) {
            vertex = new ValueVertex(expression.toLiteralExpr().get());
        } else if (expression.toFieldAccessExpr().isPresent()) {
            vertex = new ValueVertex(expression.toFieldAccessExpr().get());
        } else if (expression.toObjectCreationExpr().isPresent()) {
            vertex = new ValueVertex(expression.toObjectCreationExpr().get());
            dependencyGraph.addVertex(vertex);

            for (Expression expr : expression.toObjectCreationExpr().get().getArguments()) {
                addExpressionToDependencyGraph(expr, vertex);
            }
        } else {
            System.out.println("Could not resolve expression " + expression.toString() + " " + expression.getClass().getTypeName().toString());
        }

        if (vertex != null) {
            // Add vertices if they do not exist
            dependencyGraph.addVertex(vertex);
            dependencyGraph.addEdge(source, vertex, edge);
        }
    }


    private void resolveSetterMethod(ControlFlowVertex vertex, String scopeString, DependencyVertex source) throws Exception {
        ExpressionStmt expressionStmt = (ExpressionStmt) vertex.getNode();
        Expression expression = expressionStmt.getExpression();

        if (expression.toMethodCallExpr().isPresent()) {  // Expression is a method call
            Optional<Expression> argument = Optional.empty();

            MethodCallExpr methodCallExpr = expression.toMethodCallExpr().get();

            if (JavaParserUtils.isOptionalIfPresentSetter(methodCallExpr)) {
                MethodReferenceExpr methodReferenceExpr = methodCallExpr.getArguments().get(0).toMethodReferenceExpr().get();
                if (source instanceof TemplateFieldVertex) {
                    TemplateFieldVertex templateFieldVertex = (TemplateFieldVertex) source;
                    if (templateFieldVertex.getField().setterName.toString().equals(methodReferenceExpr.getIdentifier())) {
                        argument = Optional.of(methodCallExpr.getScope().get());
                    }
                } else if (JavaParserUtils.isSetterName(methodReferenceExpr.getIdentifier())) {
                    argument = Optional.of(methodCallExpr.getScope().get());
                }
            } else if (methodCallExpr.getScope().isPresent()) { // Method is called on some variable
                NameExpr scope = JavaParserUtils.resolveVariableName(methodCallExpr.getScope().get());

                boolean scopeIsListType = false;

                // Find out if the scope is a list type
                if (methodCallExpr.getScope().get().toMethodCallExpr().isPresent() &&
                        methodCallExpr.getScope().get().toMethodCallExpr().get().getScope().isPresent() &&
                        methodCallExpr.getScope().get().toMethodCallExpr().get().resolve().getReturnType().isReferenceType()) {
                    String resolvedType = methodCallExpr.getScope().get().toMethodCallExpr().get().resolve().getReturnType().asReferenceType().describe();
                    scopeIsListType = resolvedType.startsWith("java.util.List<");
                    //System.out.println("Resolved Type " + resolvedType + " bool " + scopeIsListType);
                }

                if ((scope != null && scope.toString().equals(scopeString)) ||
                        (methodCallExpr.getScope().get().toString().equals(scopeString) ||
                                (scopeIsListType && methodCallExpr.getScope().get().toMethodCallExpr().get().getScope().get().toString().equals(scopeString)))) {
                    // Variable matches the given variable name
                    if (source instanceof TemplateFieldVertex) {
                        TemplateFieldVertex templateFieldVertex = (TemplateFieldVertex) source;

                        if (templateFieldVertex.getField().setterName.equals(methodCallExpr.getName()) ||
                                (methodCallExpr.getName().toString().equals("add") &&
                                        methodCallExpr.getScope().get().toMethodCallExpr().isPresent() &&
                                        methodCallExpr.getScope().get().toMethodCallExpr().get().getName().equals(templateFieldVertex.getField().getterName))) {

                            // Method equals the desired setter method. Either var.setXY(ab) or var.getElement().add(ab)
                            argument = Optional.of(methodCallExpr.getArguments().get(0));
                        }
                    } else if (methodCallExpr.getName().toString().startsWith("set") || methodCallExpr.getName().toString().equals("add")) {
                        argument = Optional.of(methodCallExpr.getArguments().get(0));
                    }

                } else {
                    System.out.println("No name expr " + expression.toString());
                }
            }

            if (argument.isPresent()) {
                this.addExpressionToDependencyGraph(argument.get(), source);
                this.resolveContainingIfStatements(vertex, source);
            }
        }
    }

    private void resolveContainingIfStatements(ControlFlowVertex vertex, DependencyVertex source) throws Exception {

        ControlFlowVertex parent = vertex.getParent();

        while (parent != null) {
            Node node = parent.getNode();

            if (node instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) node;

                Expression condition = ifStmt.getCondition();

                for (Expression expr : JavaParserUtils.resolveAllExpressionsInCondition(condition)) {
                    this.addExpressionToDependencyGraph(expr, source, new DependencyEdge(true));
                }
            } else if (node instanceof SwitchStmt) {

                SwitchStmt switchStmt = (SwitchStmt) node;

                for (Expression expr : JavaParserUtils.resolveAllExpressionsInCondition(switchStmt.getSelector())) {
                    this.addExpressionToDependencyGraph(expr, source, new DependencyEdge(true));
                }
            }

            parent = parent.getParent();
        }
    }

    private void resolveFields(VariableVertex variableVertex, TemplateField field) throws Exception {

        DependencyVertex fieldVertex = new TemplateFieldVertex(field);
        this.dependencyGraph.addVertex(fieldVertex);

        for (ReachingDefinitions.Assignment assignment : this.assignments) {

            if (assignment.variable == null || !assignment.variable.equals(variableVertex.getVariableName())) continue;

            ControlFlowVertex vertex = assignment.vertex;
            Node node = vertex.getNode();

            if (node instanceof ExpressionStmt) {
                ExpressionStmt expressionStmt = (ExpressionStmt) node;
                Expression expression = expressionStmt.getExpression();

                if (expression.toMethodCallExpr().isPresent()) {
                    this.resolveSetterMethod(vertex, variableVertex.getVariableName().toString(), fieldVertex);
                } else {
                    if (!expression.toVariableDeclarationExpr().isPresent() && !expression.toAssignExpr().isPresent()) {
                        System.out.println("Did not resolve: " + node + " for exportable " + node.getClass().getTypeName());
                    }
                }
            }
        }
    }

    private void resolveDependencies(VariableVertex variableVertex) throws Exception {

        boolean isTemplateClassVariableVertex = this.getVariableType(variableVertex) != null;

        for (ReachingDefinitions.Assignment assignment : this.assignments) {
            if (assignment.variable == null || !assignment.variable.equals(variableVertex.getVariableName())) continue;

            ControlFlowVertex vertex = assignment.vertex;
            Node node = vertex.getNode();

            if (node instanceof ExpressionStmt) {
                ExpressionStmt expressionStmt = (ExpressionStmt) node;
                Expression expression = expressionStmt.getExpression();

                if (expression.toMethodCallExpr().isPresent()) {
                    if (!isTemplateClassVariableVertex) {
                        resolveSetterMethod(vertex, variableVertex.getVariableName().toString(), variableVertex);
                    }
                } else if (expression.toAssignExpr().isPresent()) { // id += observation.getId()
                    AssignExpr assignExpr = expression.toAssignExpr().get();

                    if (assignExpr.getTarget().toNameExpr().isPresent() &&
                            assignExpr.getTarget().toNameExpr().get().getName().equals(variableVertex.getVariableName())) {

                        this.addExpressionToDependencyGraph(assignExpr.getValue(), variableVertex);
                        this.resolveContainingIfStatements(vertex, variableVertex);
                    }
                } else if (expression.toVariableDeclarationExpr().isPresent()) { // Long id = observation.getId();

                    VariableDeclarationExpr declarationExpr = expression.toVariableDeclarationExpr().get();
                    for (VariableDeclarator vd : declarationExpr.getVariables()) {
                        if (vd.getName().equals(variableVertex.getVariableName())) {
                            if (vd.getInitializer().isPresent()) {
                                this.dependencyGraph.addVertex(variableVertex);
                                this.addExpressionToDependencyGraph(vd.getInitializer().get(), variableVertex);
                                this.resolveContainingIfStatements(vertex, variableVertex);
                            }
                        }
                    }

                } else {
                    System.out.println(node + " not resolvable");
                }
            } else if (node instanceof ForEachStmt) {
                ForEachStmt forEachStmt = (ForEachStmt) node;
                this.addExpressionToDependencyGraph(forEachStmt.getIterable(), variableVertex);
                this.resolveContainingIfStatements(vertex, variableVertex);
            }
        }
    }


    private TemplateClass getVariableType(VariableVertex variableVertex) throws Exception {


        return getVariableType(variableVertex.getResolvedType());
    }

    private TemplateClass getVariableType(ResolvedType resolvedType) throws Exception {
        Optional<TemplateClass> templateClassDataType = Optional.empty();

        if (resolvedType.isReferenceType() &&
                JavaParserUtils.isCompositionType(resolvedType.asReferenceType().getQualifiedName())) {
            templateClassDataType = this.templateClasses.stream().filter(cls -> resolvedType.asReferenceType().describe().endsWith("." + cls.className.asString())).findFirst();

            if (!templateClassDataType.isPresent()) {
                throw new Exception("Strange reference type behavior " +
                        resolvedType.asReferenceType().describe());
            }
        }

        return templateClassDataType.orElse(null);
    }
}
