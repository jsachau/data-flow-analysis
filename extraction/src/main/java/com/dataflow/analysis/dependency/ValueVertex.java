package com.dataflow.analysis.dependency;


import com.github.javaparser.ast.expr.Expression;

public class ValueVertex extends DependencyVertex {

    private Expression value;

    public ValueVertex(Expression literal) {
        this.value = literal;
    }

    public Expression getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Value: " + this.value.toString();
    }
}
