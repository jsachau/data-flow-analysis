package com.dataflow.analysis.dependency;

import com.github.javaparser.ast.body.MethodDeclaration;

public class MethodDependencyGraph extends DependencyGraph {

    private MethodDeclaration method;

    public MethodDependencyGraph(MethodDeclaration method) {
        super();
        this.method = method;
    }

}
