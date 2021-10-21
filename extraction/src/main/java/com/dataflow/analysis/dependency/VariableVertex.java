package com.dataflow.analysis.dependency;

import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.Comparator;

public class VariableVertex extends DependencyVertex {

    private SimpleName variableName;
    private ResolvedType resolvedType;

    private boolean isArgument;
    private int argumentPosition;


    private boolean isFHIRProfile;

    public static Comparator<VariableVertex> byArgumentPosition = Comparator.comparing(v -> v.argumentPosition);

    public VariableVertex(SimpleName variableName, ResolvedType type) {
        this.variableName = variableName;
        this.resolvedType = type;
        this.isFHIRProfile = false;

        this.isArgument = false;
        this.argumentPosition = -1;

    }

    public VariableVertex(SimpleName variableName, ResolvedType type, boolean isArgument, int argumentPosition) {
        this(variableName, type);

        this.isArgument = isArgument;
        this.argumentPosition = argumentPosition;
    }

    public SimpleName getVariableName() {
        return variableName;
    }

    @Override
    public String toString() {
        return this.variableName.toString();
    }

    @Override
    public int hashCode() {
        return this.variableName.toString().hashCode();
    }

    public void setIsFHIRProfile(boolean fhirProfile) {
        isFHIRProfile = fhirProfile;
    }

    public boolean isFHIRProfile() {
        return isFHIRProfile;
    }

    public boolean isArgument() {
        return isArgument;
    }

    public int getArgumentPosition() {
        return argumentPosition;
    }

    public ResolvedType getResolvedType() {
        return resolvedType;
    }

    @Override
    public boolean equals(Object obj) {

        if(obj instanceof DependencyVertex && !super.equals(obj)) return false;

        if (obj instanceof VariableVertex) {
            return this.variableName.toString().equals(((VariableVertex) obj).variableName.toString());
        }
        return super.equals(obj);
    }


}
