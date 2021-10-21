package com.dataflow.analysis.dependency;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyGraph {

    private Graph<DependencyVertex, DependencyEdge> graph;

    public DependencyGraph(Graph<DependencyVertex, DependencyEdge> graph) {
        this.graph = graph;
    }

    public DependencyGraph() {
        this.graph = new DefaultDirectedGraph<>(DependencyEdge.class);
    }

    public void addVertex(DependencyVertex vertex) {
        vertex.setOriginalDependencyGraph(this);
        this.graph.addVertex(vertex);
    }

    public void addEdge(DependencyVertex v, DependencyVertex v2) {
        this.graph.addEdge(v, v2);
    }

    public void addEdge(DependencyVertex v, DependencyVertex v2, DependencyEdge e) {
        this.graph.addEdge(v, v2, e);
    }

    public Set<DependencyVertex> vertexSet(){
        return graph.vertexSet();
    }

    public Set<DependencyVertex> methodVertexSet(){
        return this.vertexSet().stream().filter(vertex -> vertex.getOriginalDependencyGraph() == this).collect(Collectors.toSet());
    }

    public Graph<DependencyVertex, DependencyEdge> toJGraph() {
        return this.graph;
    }

    public void addGraph(DependencyGraph other) {

        Graph<DependencyVertex, DependencyEdge> newGraph = new DefaultDirectedGraph<>(DependencyEdge.class);

        Graphs.addGraph(newGraph, this.toJGraph());
        Graphs.addGraph(newGraph, other.toJGraph());

        this.graph = newGraph;
    }

    public List<VariableVertex> getArgumentVertices() {

        List<VariableVertex> vertices = new ArrayList<>();

        for(DependencyVertex vertex: this.methodVertexSet()) {
            if(vertex instanceof VariableVertex) {
                VariableVertex variableVertex = (VariableVertex) vertex;

                if(variableVertex.isArgument())
                {
                    vertices.add(variableVertex);
                }
            }
        }

        vertices.sort(VariableVertex.byArgumentPosition);

        return vertices;
    }

    public Optional<ReturnVertex> getReturnVertex() {

        for(DependencyVertex vertex: this.methodVertexSet())
        {
            if(vertex instanceof ReturnVertex)
            {
                return Optional.of((ReturnVertex) vertex);
            }
        }

        return Optional.empty();
    }

}
