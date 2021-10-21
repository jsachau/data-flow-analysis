package com.dataflow.generation.controlflow;

import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.List;

class NodeEndpoint {
    /**
     * This class handles the endpoints for a block or statement
     * so that edges of the parent can be added correctly
     */

    Node startNode;
    List<Node> endNodes;

    NodeEndpoint() {
        this.endNodes = new ArrayList<>();
    }

    void mergeEndNodes(NodeEndpoint endpoint) {
        endNodes.addAll(endpoint.endNodes);
    }

}
