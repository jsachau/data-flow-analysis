package com.dataflow.analysis;

import com.dataflow.analysis.utils.JavaParserUtils;
import com.dataflow.generation.controlflow.ControlFlowEdge;
import com.dataflow.generation.controlflow.ControlFlowVertex;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import org.jgrapht.Graph;

import java.util.*;

public class ReachingDefinitions {

    private Graph<ControlFlowVertex, ControlFlowEdge> controlFlow;


    ReachingDefinitions(Graph<ControlFlowVertex, ControlFlowEdge> controlFlow) {
        this.controlFlow = controlFlow;
    }

    class Assignment {
        ControlFlowVertex vertex;
        SimpleName variable;
        Expression scope;

        Assignment(ControlFlowVertex vertex, SimpleName variable) {
            this.vertex = vertex;
            this.variable = variable;
            this.scope = null;
        }

        Assignment(ControlFlowVertex vertex, SimpleName variable, Expression scope) {
            this.vertex = vertex;
            this.variable = variable;
            this.scope = scope;
        }

        Assignment(ControlFlowVertex vertex, Expression scope) {
            this.vertex = vertex;
            this.variable = null;
            this.scope = scope;
        }

        @Override
        public String toString() {
            //return this.variable + " (" + System.identityHashCode(this.vertex) + ")";
            return this.variable + " (" + this.vertex + ")";
        }

        @Override
        public int hashCode() {
            return this.vertex.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Assignment) {
                return this.vertex.equals(((Assignment) obj).vertex);
            }
            return super.equals(obj);
        }
    }

    class TransferFunction {
        ControlFlowVertex vertex;
        Assignment assignment;

        boolean isModification;


        TransferFunction(ControlFlowVertex vertex) {
            this.vertex = vertex;
            this.isModification = false;
            this.assignment = null;

            Node node = vertex.getNode();

            if (node instanceof ExpressionStmt) {
                ExpressionStmt expressionStmt = (ExpressionStmt) node;

                if (expressionStmt.getExpression().toVariableDeclarationExpr().isPresent()) {
                    // Type var = new Type();
                    this.assignment = new Assignment(vertex,
                            expressionStmt.getExpression().toVariableDeclarationExpr().get().getVariables().get(0).getName());
                } else if (expressionStmt.getExpression().toAssignExpr().isPresent() &&
                        expressionStmt.getExpression().toAssignExpr().get().getTarget().toNameExpr().isPresent()) {
                    // var = xyz;
                    this.isModification = true;
                    this.assignment = new Assignment(vertex,
                            expressionStmt.getExpression().toAssignExpr().get().getTarget().toNameExpr().get().getName());
                } else if (expressionStmt.getExpression().toMethodCallExpr().isPresent()) {

                    MethodCallExpr methodCallExpr = expressionStmt.getExpression().toMethodCallExpr().get();
                    if(JavaParserUtils.isOptionalIfPresentSetter(methodCallExpr)) {
                        MethodReferenceExpr methodReferenceExpr = methodCallExpr.getArguments().get(0).toMethodReferenceExpr().get();

                        if(methodReferenceExpr.getScope() != null && methodReferenceExpr.getScope().toTypeExpr().isPresent()) {
                            this.isModification = true;
                            this.assignment = new Assignment(vertex, new SimpleName(methodReferenceExpr.getScope().toTypeExpr().get().getType().asString()));
                        }
                    } else if(JavaParserUtils.isSetterName(methodCallExpr.getName()))
                    {
                        // var.setXy() / var.add() , i.e. modifies the variable but not necessarily overwrites other contents.
                        NameExpr nameExpr = JavaParserUtils.resolveVariableName(expressionStmt.getExpression().toMethodCallExpr().get());

                        if(nameExpr == null) {
                            if(methodCallExpr.getScope().isPresent()) this.assignment = new Assignment(vertex, methodCallExpr.getScope().get());
                        } else {
                            this.isModification = true;
                            this.assignment = new Assignment(vertex, nameExpr.getName());
                        }
                    }
                } else {
                    //System.out.println(((ExpressionStmt) node).getExpression() + ": " + ((ExpressionStmt) node).getExpression().getClass().getTypeName());
                }
            } else if (node instanceof ForEachStmt && ((ForEachStmt) node).toForEachStmt().isPresent()) {
                ForEachStmt stmt = ((ForEachStmt) node).toForEachStmt().get();
                this.assignment =  new Assignment(vertex,stmt.getVariable().getVariables().get(0).getName());
            }
        }

        void gen(Map<ControlFlowVertex, Set<Assignment>> previousGeneration, Set<Assignment> newAssignments) {
            if (this.assignment != null) {
                newAssignments.add(this.assignment);
            }
        }

        void kill(Map<ControlFlowVertex, Set<Assignment>> previousGeneration, Set<Assignment> newAssignments) {
            Set<Assignment> Xi = new HashSet<>(previousGeneration.get(this.vertex));

            if (this.assignment != null && this.assignment.variable != null && !this.isModification) {
                Xi.removeIf(assignment -> assignment.variable.equals(this.assignment.variable));
            }

            newAssignments.addAll(Xi);
        }
    }


    Set<Assignment> performFixedPointAnalysis() {

        Map<ControlFlowVertex, List<TransferFunction>> systemOfEquations = new HashMap<>();
        Map<ControlFlowVertex, Set<Assignment>> lastGeneration = new HashMap<>();

        for (ControlFlowVertex vertex : this.controlFlow.vertexSet()) {

            systemOfEquations.put(vertex, new ArrayList<>());
            lastGeneration.put(vertex, new HashSet<>());

            for (ControlFlowEdge e : this.controlFlow.incomingEdgesOf(vertex)) {
                systemOfEquations.get(vertex).add(new TransferFunction((ControlFlowVertex) e.getSource()));
            }
        }

        boolean reachedFixedPoint = false;
        int fixedPointCounter = 0;

        while(!reachedFixedPoint) {

            Map<ControlFlowVertex, Set<Assignment>> nextGeneration = new HashMap<>();

            for (ControlFlowVertex vertex : this.controlFlow.vertexSet()) {

                Set<Assignment> newAssignments = new HashSet<>();

                for (TransferFunction transferFunction : systemOfEquations.get(vertex)) {
                    transferFunction.kill(lastGeneration, newAssignments);
                    transferFunction.gen(lastGeneration, newAssignments);
                }

                nextGeneration.put(vertex, newAssignments);

            }

            // Check if fixed point is reached
            reachedFixedPoint = true;

            for(ControlFlowVertex vertex : nextGeneration.keySet()){
                if(!lastGeneration.get(vertex).equals(nextGeneration.get(vertex))) {
                    reachedFixedPoint = false;
                    break;
                }
            }

            lastGeneration = nextGeneration;
            fixedPointCounter++;
        }

        Set<Assignment> reachingDefinitionResults = new HashSet<>();

        for (ControlFlowVertex vertex : this.controlFlow.vertexSet()) {

            if (this.controlFlow.outgoingEdgesOf(vertex).size() == 0) {

                reachingDefinitionResults.addAll(lastGeneration.get(vertex));
            }

            //System.out.println(vertex.getNode().getClass().getTypeName() +":" + lastGeneration.get(vertex));
        }

        //System.out.println(reachingDefinitionResults);

        return reachingDefinitionResults;
    }


}
