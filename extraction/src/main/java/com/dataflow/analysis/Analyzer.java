package com.dataflow.analysis;

import com.dataflow.exportable.Mapping;
import com.dataflow.exportable.TemplateClass;
import com.dataflow.generation.CompositionGenerator;
import com.dataflow.generation.ControlFlowGenerator;
import com.dataflow.generation.controlflow.ControlFlow;
import com.github.javaparser.ast.expr.SimpleName;

import java.util.List;
import java.util.Optional;

public class Analyzer {
    /**
     * Analysis of the data flow. Gets a mapping class and a exportable directory and extracts the mapping function
     * from the mapping class.
     */

    private static SimpleName MAPPING_FUNCTION = new SimpleName("toComposition");

    private String mappingName;
    private String mappingClassPath;

    public Analyzer(String mappingName, String mappingClassPath) {
        this.mappingName = mappingName;
        this.mappingClassPath = mappingClassPath;
    }


    public Optional<Mapping> run() throws Exception {
        //Find the mapping function with the name MAPPING_FUNCTION

        ControlFlow controlFlow = ControlFlowGenerator.generate(this.mappingClassPath, MAPPING_FUNCTION);


        CompositionGenerator compositionGenerator =
                new CompositionGenerator(controlFlow.compositionName.asString());

        List<TemplateClass> templateClasses = compositionGenerator.templateClasses;


        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(controlFlow, templateClasses);
        methodAnalyzer.run();

        return Optional.of(new Mapping(this.mappingName, templateClasses, compositionGenerator.mappingInfo));
    }
}
