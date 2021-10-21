package com.dataflow;

import com.dataflow.analysis.Analyzer;
import com.dataflow.exportable.Mapping;
import com.dataflow.analysis.dependency.DependencyGraph;
import com.dataflow.analysis.dependency.DependencyVertex;
import com.dataflow.generation.MappingExtractor;
import com.github.javaparser.ast.expr.SimpleName;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class DataFlowAnalysis {

    public static <VertexType, EdgeType> void createAndShowGui(Graph<VertexType, EdgeType> g) throws Exception {
        JFrame frame = new JFrame("DemoGraph");

        String[] mColors = {
                "#39add1", // light blue
                "#3079ab", // dark blue
                "#c25975", // mauve
                "#e15258", // red
                "#f9845b", // orange
                "#838cc7", // lavender
                "#7d669e", // purple
                "#53bbb4", // aqua
                "#51b46d", // green
                "#e0ab18", // mustard
                "#637a91", // dark gray
                "#f092b0", // pink
                "#b7c0c7",  // light gray
        };

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JGraphXAdapter<VertexType, EdgeType> graph = new JGraphXAdapter<>(g);
        graph.setAllowDanglingEdges(false);

        Map<VertexType, mxICell> vertexMap = graph.getVertexToCellMap();

        int currentColor = 0;
        Map<DependencyGraph, Integer> graphColors = new HashMap<>();

        for (VertexType vertex : vertexMap.keySet()) {
            if (vertex instanceof DependencyVertex) {
                DependencyVertex v = (DependencyVertex) vertex;
                if (!graphColors.containsKey(v.getOriginalDependencyGraph())) {

                    if (currentColor >= mColors.length) {
                        currentColor = currentColor % mColors.length;
                    }

                    graphColors.put(v.getOriginalDependencyGraph(), currentColor++);
                }

                graph.setCellStyle("fillColor=" + mColors[graphColors.get(v.getOriginalDependencyGraph())], new Object[]{vertexMap.get(v)});
            }
        }

        mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);

        layout.execute(graph.getDefaultParent());
        mxGraphComponent component = new mxGraphComponent(graph);


        component.setAutoExtend(true);
        component.setLocation(200, 0);

        frame.getContentPane().add(component);

        frame.pack();
        //frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    /**
     * Entry point for the program.
     */
    public static void main(String[] args) throws Exception {

        MappingExtractor mappingExtractor = new MappingExtractor();
        List<MappingExtractor.MappingRequest> mappingRequests = mappingExtractor.generateRequests();

        GsonBuilder gson = new GsonBuilder();
        gson.setPrettyPrinting();
        gson.registerTypeAdapter(SimpleName.class, (JsonSerializer<SimpleName>) (simpleName, type, jsonSerializationContext) -> new JsonPrimitive(simpleName.asString()));

        List<Mapping> mappings = new ArrayList<>();

        for (MappingExtractor.MappingRequest request : mappingRequests) {

            System.out.println("Extracting: " + request.name);

            Analyzer flowAnalysis = new Analyzer(request.name, request.mappingClassPath);
            //createAndShowGui(flowAnalysis.getControlFlow().graph);

            Optional<Mapping> current = flowAnalysis.run();

            current.ifPresent(mappings::add);
        }

        try (FileWriter writer = new FileWriter(new File("../documentation/db_dump.json"))) {
            writer.write(gson.create().toJson(mappings));
        } catch (IOException e) {
            System.out.println("Could not write to file");
        }
    }
}
