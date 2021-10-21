package com.dataflow.analysis.dependency;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Optional;

public class MethodCallVertex extends DependencyVertex {

    private MethodCallExpr method;
    private MethodDeclaration methodDeclaration;

    public MethodCallVertex(MethodCallExpr method) {
        this.method = method;
        this.methodDeclaration = null;

        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = this.method.resolve();
            Optional<MethodDeclaration> md = resolvedMethodDeclaration.toAst();
            md.ifPresent(declaration -> this.methodDeclaration = declaration);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public boolean isResolvableMethod() {
        return this.methodDeclaration != null && !this.methodDeclaration.getName().asString().equals("constructFeederAudit");
    }

    public MethodCallExpr getMethodCallExpr() {
        return method;
    }

    public MethodDeclaration getMethodDeclaration() throws Exception {
        if(this.methodDeclaration != null)
            return this.methodDeclaration;

        throw new Exception("Error");
    }

    @Override
    public String toString() {
        return this.method.toString();
    }
}
