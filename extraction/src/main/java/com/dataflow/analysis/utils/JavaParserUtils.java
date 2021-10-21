package com.dataflow.analysis.utils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaParserUtils {

    public static void resolveMethodCall(MethodCallExpr methodCallExpr) {
        methodCallExpr.resolve().toAst().get();
    }

    public static SimpleName getVariableType(SimpleName variableName, CompilationUnit cu, MethodDeclaration md) {

        Optional<SimpleName> dataType = Optional.empty();


        cu.findAll(VariableDeclarationExpr.class).forEach(ae -> {
            try {
                ResolvedType resolvedType = ae.calculateResolvedType();
                System.out.println(ae.toString() + ": " + resolvedType.describe());


            } catch (UnsolvedSymbolException e) {
                System.out.println(e.getName() + " unresolvable");
            } catch (Exception e) {
                System.out.println("unknown error: " + e.getMessage());
            }
        });


        return dataType.orElse(null);
    }

    public static NameExpr resolveVariableName(Expression methodCallExpr) {
        Expression current = methodCallExpr;
        while (current instanceof MethodCallExpr) {
            if (!(((MethodCallExpr) current).getScope().isPresent())) break;
            current = ((MethodCallExpr) current).getScope().get();
        }

        if (current instanceof NameExpr) {
            return ((NameExpr) current);
        }

        return null;
    }

    public static List<Expression> resolveAllUsedExpressionsInArguments(Expression methodCallExpr) {

        List<Expression> expressions = new ArrayList<>();

        Expression current = methodCallExpr;
        while (current.toMethodCallExpr().isPresent()) {
            expressions.addAll(current.toMethodCallExpr().get().getArguments());

            if (!(((MethodCallExpr) current).getScope().isPresent())) break;
            current = ((MethodCallExpr) current).getScope().get();
        }


        return expressions;
    }

    public static List<Expression> resolveAllExpressionsInCondition(Expression condition) {

        List<Expression> expressions = new ArrayList<>();

        if (condition instanceof BinaryExpr) {
            BinaryExpr expr = (BinaryExpr) condition;
            //Explore left part
            if (expr.getLeft() instanceof BinaryExpr) {
                expressions.addAll(resolveAllExpressionsInCondition(expr.getLeft()));
            } else if (expr.getLeft().toUnaryExpr().isPresent()) {
                expressions.add(expr.getLeft().toUnaryExpr().get().getExpression());
            } else {
                expressions.add(expr.getLeft());
            }

            //Explore right part
            if (expr.getRight() instanceof BinaryExpr) {
                expressions.addAll(resolveAllExpressionsInCondition(expr.getRight()));
            } else if (expr.getRight().toUnaryExpr().isPresent()) {
                expressions.add(expr.getRight().toUnaryExpr().get().getExpression());
            } else {
                expressions.add(expr.getRight());
            }
        } else {
            expressions.add(condition);
        }

        return expressions;
    }

    public static boolean isSetterName(NameExpr nameExpr) {
        return isSetterName(nameExpr.getName());
    }

    public static boolean isSetterName(SimpleName name) {
        return name.toString().startsWith("set") || name.toString().equals("add");
    }

    public static boolean isSetterName(String name) {
        return name.startsWith("set") || name.equals("add");
    }

    public static boolean isOptional(Optional<Expression> expression) {
        return expression.filter(JavaParserUtils::isOptional).isPresent();
    }

    public static boolean isOptional(Expression expression) {
        try {
            return expression.calculateResolvedType().isReferenceType() &&
                    expression.calculateResolvedType().asReferenceType().describe().startsWith("java.util.Optional<");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isOptionalIfPresentSetter(MethodCallExpr methodCallExpr) {
        if (isOptional(methodCallExpr.getScope()) &&
                methodCallExpr.getName().toString().equals("ifPresent") &&
                methodCallExpr.getArguments().size() == 1 &&
                methodCallExpr.getArguments().get(0).toMethodReferenceExpr().isPresent()) {

            return isSetterName(methodCallExpr.getArguments().get(0).toMethodReferenceExpr().get().getIdentifier());
        }

        return false;
    }



    public static boolean isCompositionType(String qualifiedName) {
        return qualifiedName.startsWith("org.ehrbase.fhirbridge.ehr.opt.");
    }

}
