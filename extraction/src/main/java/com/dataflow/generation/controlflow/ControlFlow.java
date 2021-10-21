package com.dataflow.generation.controlflow;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ControlFlow {
    /**
     * A control flow of a single file. Contains a graph with one connected component for each method.
     */

    public Graph<ControlFlowVertex, ControlFlowEdge> graph;
    public MethodDeclaration method;

    public SimpleName compositionName;


    public ControlFlow(MethodDeclaration method){
        this.graph = new DefaultDirectedGraph<>(ControlFlowEdge.class);
        this.method = method;
    }

    public ControlFlowVertex addVertex(Node node, @Nullable ControlFlowVertex parent)
    {
        ControlFlowVertex vertex;
        if(parent == null) {
            vertex = new ControlFlowVertex(node);
        } else {
            vertex = new ControlFlowVertex(node, parent);
        }

        this.graph.addVertex(vertex);

        return vertex;
    }

    public Optional<ControlFlowVertex> getVertex(Node node) {
        return this.graph.vertexSet().stream().filter(vertex -> vertex.equals(node)).findFirst();
    }

    public void addEdge(Node node1, Node node2)
    {
        Optional<ControlFlowVertex> vertex1 = this.getVertex(node1);
        Optional<ControlFlowVertex> vertex2 = this.getVertex(node2);

        if(vertex1.isPresent() && vertex2.isPresent())
        {
            this.graph.addEdge(vertex1.get(), vertex2.get());
        }
    }

    public void addEdge(ControlFlowVertex vertex1, Node node2)
    {
        Optional<ControlFlowVertex> vertex2 = this.getVertex(node2);
        vertex2.ifPresent(vertex -> this.graph.addEdge(vertex1, vertex));
    }

    public void addEdge(Node node1, ControlFlowVertex vertex2)
    {
        Optional<ControlFlowVertex> vertex1 = this.getVertex(node1);
        vertex1.ifPresent(vertex -> this.graph.addEdge(vertex, vertex2));
    }

    public void addEdge(ControlFlowVertex vertex1, ControlFlowVertex vertex2) {
        this.graph.addEdge(vertex1, vertex2);
    }

    public Set<ControlFlowEdge> outgoingEdgesOf(Node node)
    {
        Optional<ControlFlowVertex> vertex = this.graph.vertexSet().stream().filter(v -> v.equals(node)).findFirst();

        return vertex.map(controlFlowVertex -> this.graph.outgoingEdgesOf(controlFlowVertex)).orElse(null);

    }

    public Set<ControlFlowEdge> edgesOf(Node node)
    {
        Optional<ControlFlowVertex> vertex = this.graph.vertexSet().stream().filter(v -> v.equals(node)).findFirst();

        return vertex.map(controlFlowVertex -> this.graph.edgesOf(controlFlowVertex)).orElse(null);

    }
}
