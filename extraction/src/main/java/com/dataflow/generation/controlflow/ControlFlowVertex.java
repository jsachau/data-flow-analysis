package com.dataflow.generation.controlflow;

import com.github.javaparser.ast.Node;

public class ControlFlowVertex {

    private Node node;
    private ControlFlowVertex parent;

    public ControlFlowVertex(Node node)
    {
        this.node = node;
        this.parent = null;
    }

    public ControlFlowVertex(Node node, ControlFlowVertex parent)
    {
        this.node = node;
        this.parent = parent;
    }

    public Node getNode() {
        return this.node;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ControlFlowVertex)
        {
            return ((ControlFlowVertex) obj) == this;
        } else if(obj instanceof Node)
        {
            return this.node == obj;
        }

        return false;
    }

    public ControlFlowVertex getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return this.node.toString();
    }
}
