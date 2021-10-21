package com.dataflow.exportable;

import com.github.javaparser.ast.expr.Expression;

public class ValueDependency extends Dependency{

    public String value;

    public ValueDependency(boolean logicalDependency, Expression expression) {
        super(logicalDependency, Type.VALUE);

        this.value = expression.toString();
    }
}
