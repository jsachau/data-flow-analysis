package com.dataflow.exportable;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Optional;

public class MethodCallDependency extends Dependency {

    public SimpleName method;
    public String scope;
    public String returnType;
    public String expression;
    public String methodType;

    public MethodCallDependency(boolean logicalDependency, MethodCallExpr methodCall) {
        super(logicalDependency, Type.METHOD_CALL);

        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = methodCall.resolve();
            this.returnType = resolvedMethodDeclaration.getReturnType().describe();
        } catch (Exception e) {
            System.out.println(e);
        }

        this.method = methodCall.getName();
        if(methodCall.getScope().isPresent())
        {
            this.scope = methodCall.getScope().get().toString();
        } else {
            this.scope = null;
        }

        this.expression = methodCall.toString();
    }
}
