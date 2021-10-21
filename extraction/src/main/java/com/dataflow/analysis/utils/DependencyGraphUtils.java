package com.dataflow.analysis.utils;

import com.dataflow.analysis.dependency.DependencyEdge;
import com.dataflow.analysis.dependency.DependencyVertex;
import org.jgrapht.Graph;

public class DependencyGraphUtils {

    public static void printGraph(Graph<DependencyVertex, DependencyEdge> dependencyGraph) {
        System.out.println("---- Dependency graph ----");
        System.out.println("---- Vertices ----");

        for (DependencyVertex v : dependencyGraph.vertexSet()) {
            System.out.println(v.toString());
        }

        System.out.println("---- Edges ----");

        for (DependencyEdge e : dependencyGraph.edgeSet()) {
            System.out.println(e.getSource().toString() + " -> " + e.getTarget().toString() + "\t ()");
        }

        System.out.println("---- End ----");
    }
}
