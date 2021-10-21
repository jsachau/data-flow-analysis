package com.dataflow.exportable;

import com.github.javaparser.ast.expr.SimpleName;

public class VariableDependency extends Dependency {

    public SimpleName variableName;
    public String variableType;

    public VariableDependency(boolean logicalDependency, SimpleName variableName, String variableType) {
        super(logicalDependency, Type.VARIABLE);
        this.variableName = variableName;
        this.variableType = variableType;
    }
}
