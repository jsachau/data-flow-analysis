package com.dataflow.exportable;

public abstract class Dependency {

    public enum Type {
        VARIABLE, METHOD_CALL, VALUE
    }

    public boolean logicalDependency;
    public Type type;

    public Dependency(boolean logicalDependency, Type type) {
        this.logicalDependency = logicalDependency;
        this.type = type;
    }
}
