package com.dataflow.analysis.dependency;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class ReturnVertex extends DependencyVertex {

    private ResolvedType resolvedType;
    private Type type;

    public ReturnVertex(Type type) {
        this.type = type;


        try {
            this.resolvedType = this.type.resolve();
        } catch(IllegalStateException ex) { }
    }


    public Type getType() {
        return type;
    }

    public ResolvedType getResolvedType() {
        return resolvedType;
    }

    @Override
    public String toString() {
        return "Return of " + this.type.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
