package com.dataflow.analysis.dependency;


public abstract class DependencyVertex {

    private DependencyGraph originalDependencyGraph;

    public void setOriginalDependencyGraph(DependencyGraph originalDependencyGraph) {
        this.originalDependencyGraph = originalDependencyGraph;
    }

    public DependencyGraph getOriginalDependencyGraph() {
        return originalDependencyGraph;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DependencyVertex)
        {
            return this.originalDependencyGraph == ((DependencyVertex) obj).originalDependencyGraph;
        }

        return false;
    }
}