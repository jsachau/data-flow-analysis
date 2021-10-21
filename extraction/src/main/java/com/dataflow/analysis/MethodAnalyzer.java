package com.dataflow.analysis;

import com.dataflow.analysis.dependency.*;
import com.dataflow.exportable.*;
import com.dataflow.generation.controlflow.ControlFlow;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MethodAnalyzer {

    private ControlFlow controlFlow;

    private List<TemplateClass> templateClasses;


    MethodAnalyzer(ControlFlow controlFlow, List<TemplateClass> templateClasses) throws Exception {
        this.controlFlow = controlFlow;
        this.templateClasses = templateClasses;
    }

    public DependencyGraph createDependencyGraph() throws Exception {
        DependencyGraphBuilder dependencyGraphBuilder =
                new DependencyGraphBuilder(this.controlFlow,
                        this.templateClasses);

        return dependencyGraphBuilder.build();
    }

    public void run() throws Exception {

        DependencyGraph dependencyGraph = this.createDependencyGraph();
        //ControlFlowGUI.createAndShowGui(dependencyGraph.toJGraph());

        ReturnVertex returnVariableVertex = getReturnVariableVertex(dependencyGraph);
        VariableVertex fhirProfileVertex = getFHIRProfileVertex(dependencyGraph);


        for (DependencyVertex vertex : dependencyGraph.vertexSet()) {
            if (vertex instanceof TemplateFieldVertex) {

                TemplateField field = ((TemplateFieldVertex) vertex).getField();

                addMappings(dependencyGraph, field, vertex, fhirProfileVertex);
            }
        }
    }


    static void addMappings(DependencyGraph dependencyGraph, TemplateField field, DependencyVertex source, VariableVertex target) throws Exception {

        AllDirectedPaths<DependencyVertex, DependencyEdge> allDirectedPaths = new AllDirectedPaths<>(dependencyGraph.toJGraph());

        for (GraphPath<DependencyVertex, DependencyEdge> path :
                allDirectedPaths.getAllPaths(source,
                        target,
                        true,
                        dependencyGraph.vertexSet().size())) {

            TemplateFieldMapping mapping = new TemplateFieldMapping();

            for (DependencyEdge e : path.getEdgeList()) {

                if (e.getTarget() instanceof VariableVertex) {
                    VariableVertex vertex = (VariableVertex) e.getTarget();
                    mapping.dependencies.add(new VariableDependency(e.isLogicalDependency(), vertex.getVariableName(), vertex.getResolvedType().describe()));
                } else if (e.getTarget() instanceof MethodCallVertex) {
                    MethodCallVertex vertex = (MethodCallVertex) e.getTarget();
                    mapping.dependencies.add(new MethodCallDependency(e.isLogicalDependency(),
                            vertex.getMethodCallExpr()));
                } else if (e.getTarget() instanceof ValueVertex) {
                    ValueVertex vertex = (ValueVertex) e.getTarget();
                    mapping.dependencies.add(new ValueDependency(e.isLogicalDependency(), vertex.getValue()));
                }
            }

            if(!mapping.dependencies.isEmpty()) {
                field.observationTemplateFieldMappings.add(mapping);
            }
        }
    }

    static ReturnVertex getReturnVariableVertex(DependencyGraph dependencyGraph) throws Exception {
        Optional<DependencyVertex> returnVariableVertex = dependencyGraph.vertexSet().stream().filter(
                v -> v instanceof ReturnVertex && v.getOriginalDependencyGraph().equals(dependencyGraph)).findFirst();

        if (!returnVariableVertex.isPresent()) {
            throw new Exception("");
        }

        return (ReturnVertex) returnVariableVertex.get();
    }

    static VariableVertex getFHIRProfileVertex(DependencyGraph dependencyGraph) throws Exception {
        Supplier<Stream<DependencyVertex>> argumentStreamSupplier = () -> dependencyGraph.vertexSet().stream().filter(
                v -> v instanceof VariableVertex && ((VariableVertex) v).isArgument() &&
                        v.getOriginalDependencyGraph().equals(dependencyGraph));

        Optional<DependencyVertex> fhirProfileVertex = argumentStreamSupplier.get().findFirst();

        if (argumentStreamSupplier.get().count() != 1 || !fhirProfileVertex.isPresent()) {
            throw new Exception("");
        }

        return (VariableVertex) fhirProfileVertex.get();
    }
}
