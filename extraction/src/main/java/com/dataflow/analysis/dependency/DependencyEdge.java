package com.dataflow.analysis.dependency;

import com.github.javaparser.ast.expr.Expression;
import org.jgrapht.graph.DefaultEdge;

public class DependencyEdge extends DefaultEdge {

    private boolean logicalDependency;

    public DependencyEdge() {
        this.logicalDependency = false;
    }

    public DependencyEdge(boolean logicalDependency) {
        this.logicalDependency = logicalDependency;
    }


    public boolean isLogicalDependency() {
        return logicalDependency;
    }

    @Override
    public String toString()
    {
        return "";
    }

    @Override
    public Object getSource()
    {
        return super.getSource();
    }

    @Override
    public Object getTarget()
    {
        return super.getTarget();
    }
}
