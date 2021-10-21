package com.dataflow.generation.controlflow;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

public class NodeHandler {

    public ControlFlow controlFlow;

    public NodeHandler(ControlFlow controlFlow) {
        this.controlFlow = controlFlow;
    }

    /**
     * Handles a specific node and adds it to the control flow graph if necessary.
     *
     * @param node The node
     * @return Endpoints and start point of the node (which may be in the body of the node)
     */
    public NodeEndpoint handleNode(Node node, @Nullable ControlFlowVertex parent) {
        NodeEndpoint nodeEndpoints = null;

        if (node instanceof ForStmt) {
            nodeEndpoints = fillForLoop((ForStmt) node, parent);
        } else if (node instanceof ForEachStmt) {
            nodeEndpoints = fillForEachLoop((ForEachStmt) node, parent);
        } else if (node instanceof IfStmt) {
            nodeEndpoints = fillIfStatement((IfStmt) node, parent);
        } else if (node instanceof TryStmt) {
            nodeEndpoints = fillTryStatement((TryStmt) node, parent);
        } else if (node instanceof BlockStmt) {
            nodeEndpoints = fillBlockStmt((BlockStmt) node, parent);
        } else if (node instanceof MethodDeclaration) {
            nodeEndpoints = fillMethod((MethodDeclaration) node, parent);
        } else if (node instanceof SwitchStmt) {
            nodeEndpoints = fillSwitchStatement((SwitchStmt) node, parent);
        } else if (node instanceof ThrowStmt) {
            nodeEndpoints = new NodeEndpoint();
            nodeEndpoints.startNode = node;
            this.controlFlow.addVertex(node, parent);
        } else if (!(node instanceof LineComment)) {
            nodeEndpoints = new NodeEndpoint();
            nodeEndpoints.startNode = node;
            nodeEndpoints.endNodes.add(node);
            this.controlFlow.addVertex(node, parent);
        }

        return nodeEndpoints;
    }

    private NodeEndpoint fillMethod(MethodDeclaration node, @Nullable ControlFlowVertex parent) {

        NodeEndpoint endpoint = new NodeEndpoint();
        endpoint.startNode = node;
        ControlFlowVertex methodVertex = this.controlFlow.addVertex(node, parent);

        if (node.getBody().isPresent()) {
            NodeEndpoint bodyEndpoint = fillBody(node.getBody().get().getChildNodes(), methodVertex);
            this.controlFlow.addEdge(node, bodyEndpoint.startNode);
            endpoint.endNodes = bodyEndpoint.endNodes;
        }

        // Adding Skip Nodes (Empty Returns) if endpoints are no return statements
        List<Node> newEndpoints = new ArrayList<>();

        for (Node end : endpoint.endNodes) {
            if (end instanceof ReturnStmt || end instanceof ThrowStmt) {
                newEndpoints.add(end);
            } else {
                ReturnStmt returnStmt = new ReturnStmt();
                this.controlFlow.addVertex(returnStmt, methodVertex);
                this.controlFlow.addEdge(end, returnStmt);
                newEndpoints.add(returnStmt);
            }
        }

        endpoint.endNodes = newEndpoints;

        return endpoint;
    }

    private NodeEndpoint fillSwitchStatement(SwitchStmt node, @Nullable ControlFlowVertex parent) {
        NodeEndpoint nodeEndpoint = new NodeEndpoint();

        ControlFlowVertex switchVertex = this.controlFlow.addVertex(node, parent);
        nodeEndpoint.startNode = node;

        Optional<ControlFlowVertex> waterfallEntry = Optional.empty();

        for (SwitchEntry entry : node.getEntries()) {

            ControlFlowVertex entryVertex = this.controlFlow.addVertex(entry, switchVertex);
            // Switch jumps directly to case
            this.controlFlow.addEdge(node, entryVertex);

            // In case of an empty case before
            if (waterfallEntry.isPresent()) {
                this.controlFlow.addEdge(waterfallEntry.get(), entryVertex);
                waterfallEntry = Optional.empty();
            }

            if (!entry.getStatements().isEmpty()) {
                List<Node> entries = new ArrayList<>(entry.getStatements());

                NodeEndpoint entryEndpoints = fillBody(entries, switchVertex);

                this.controlFlow.addEdge(entry, entryEndpoints.startNode);

                if (entryEndpoints.endNodes.size() == 1 && ((Statement) entryEndpoints.endNodes.get(0)).isBreakStmt()) {
                    nodeEndpoint.mergeEndNodes(entryEndpoints);
                } else if (entryEndpoints.endNodes.size() == 1) {
                    // Set Waterfall entry only if the method does not end within the case
                    waterfallEntry = this.controlFlow.getVertex(entryEndpoints.endNodes.get(0));
                } else if (entryEndpoints.endNodes.size() > 1) {
                    System.out.println("Switch statement entry has more than one endnode " + entryEndpoints);
                }
            } else {
                waterfallEntry = Optional.of(entryVertex);
            }
        }

        waterfallEntry.ifPresent(vertex -> nodeEndpoint.endNodes.add(vertex.getNode()));

        return nodeEndpoint;
    }

    private NodeEndpoint fillForLoop(ForStmt node, @Nullable ControlFlowVertex parent) {
        NodeEndpoint nodeEndpoint = new NodeEndpoint();

        ControlFlowVertex forLoopStartVertex = this.controlFlow.addVertex(node, parent);

        NodeEndpoint bodyEndpoint = fillBody(node.getBody().getChildNodes(), forLoopStartVertex);

        this.controlFlow.addEdge(node, bodyEndpoint.startNode);

        for (Node current : bodyEndpoint.endNodes) {
            this.controlFlow.addEdge(current, node);
        }

        nodeEndpoint.startNode = node;
        nodeEndpoint.endNodes.add(node);

        return nodeEndpoint;
    }

    private NodeEndpoint fillForEachLoop(ForEachStmt node, @Nullable ControlFlowVertex parent) {
        NodeEndpoint nodeEndpoint = new NodeEndpoint();

        ControlFlowVertex forLoopStartVertex = this.controlFlow.addVertex(node, parent);

        NodeEndpoint bodyEndpoint = fillBody(node.getBody().getChildNodes(), forLoopStartVertex);

        this.controlFlow.addEdge(node, bodyEndpoint.startNode);

        for (Node current : bodyEndpoint.endNodes) {
            this.controlFlow.addEdge(current, node);
        }

        nodeEndpoint.startNode = node;
        nodeEndpoint.endNodes.add(node);

        return nodeEndpoint;
    }


    private NodeEndpoint fillIfStatement(IfStmt node, @Nullable ControlFlowVertex parent) {
        NodeEndpoint endpoint = new NodeEndpoint();
        ControlFlowVertex ifNodeVertex = this.controlFlow.addVertex(node, parent);

        endpoint.startNode = node;

        // An if block has two child nodes, one for its statement and the second one for its contents
        Node ifBlock = node.getChildNodes().get(1);

        if (!ifBlock.getChildNodes().isEmpty()) {
            NodeEndpoint bodyEndpoint = fillBody(ifBlock.getChildNodes(), ifNodeVertex);
            this.controlFlow.addEdge(node, bodyEndpoint.startNode);
            endpoint.mergeEndNodes(bodyEndpoint);
        }


        if (node.getElseStmt().isPresent()) {
            NodeEndpoint elseEndpoints = handleNode(node.getElseStmt().get(), ifNodeVertex);
            endpoint.mergeEndNodes(elseEndpoints);

            this.controlFlow.addEdge(node, elseEndpoints.startNode);
        } else {
            // In case no else node is present the control flow will continue directly from the if
            endpoint.endNodes.add(endpoint.startNode);
        }

        return endpoint;
    }

    private NodeEndpoint fillBlockStmt(BlockStmt node, @Nullable ControlFlowVertex parent) {
        if (node.getChildNodes().isEmpty()) {
            return null;
        }

        return fillBody(node.getChildNodes(), parent);
    }

    private NodeEndpoint fillTryStatement(TryStmt node, @Nullable ControlFlowVertex parent) {
        NodeEndpoint endpoint = new NodeEndpoint();

        if (node.getChildNodes().isEmpty()) {
            return null;
        }

        endpoint.startNode = node;
        ControlFlowVertex tryStartVertex = this.controlFlow.addVertex(node, parent);

        NodeEndpoint tryEndpoints = handleNode(node.getTryBlock(), tryStartVertex);

        this.controlFlow.addEdge(node, tryEndpoints.startNode);

        List<NodeEndpoint> catchEndpoints = new ArrayList<>();

        for (CatchClause catchClause : node.getCatchClauses()) {
            NodeEndpoint catchEndpoint = handleNode(catchClause.getBody(), tryStartVertex);
            catchEndpoints.add(catchEndpoint);
            this.controlFlow.addEdge(node, catchEndpoint.startNode);
        }

        if (node.getFinallyBlock().isPresent()) {
            // TODO: Add Finally block
        } else {
            endpoint.mergeEndNodes(tryEndpoints);
            for (NodeEndpoint catchEndpoint : catchEndpoints) {
                endpoint.mergeEndNodes(catchEndpoint);
            }
        }

        return endpoint;
    }

    /**
     * Fills the graph with a list of nodes and sets edges between start and endnotes according to
     * the control flow.
     */
    private NodeEndpoint fillBody(List<Node> nodes, @Nullable ControlFlowVertex parent) {

        NodeEndpoint bodyEndpoint = new NodeEndpoint();
        List<NodeEndpoint> endpoints = new ArrayList<>();


        {
            ListIterator<Node> iterator = nodes.listIterator();
            Node current;

            while (iterator.hasNext()) {
                current = iterator.next();
                NodeEndpoint endpoint = handleNode(current, parent);
                if (endpoint != null) endpoints.add(endpoint);
            }
        }

        {
            ListIterator<NodeEndpoint> nodeEndpointsIterator = endpoints.listIterator();
            NodeEndpoint currentEndpoint;

            while (nodeEndpointsIterator.hasNext()) {
                currentEndpoint = nodeEndpointsIterator.next();

                if (nodeEndpointsIterator.hasNext()) {
                    NodeEndpoint nextEndpoint = nodeEndpointsIterator.next();

                    for (Node node : currentEndpoint.endNodes) {
                        this.controlFlow.addEdge(node, nextEndpoint.startNode);
                    }

                    nodeEndpointsIterator.previous();
                }
            }
        }

        if (!endpoints.isEmpty()) {
            bodyEndpoint.startNode = endpoints.get(0).startNode;
            bodyEndpoint.endNodes = endpoints.get(endpoints.size() - 1).endNodes;
        }

        return bodyEndpoint;
    }

}
