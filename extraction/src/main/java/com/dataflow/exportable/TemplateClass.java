package com.dataflow.exportable;

import com.github.javaparser.ast.expr.SimpleName;

import java.util.ArrayList;
import java.util.List;

public class TemplateClass {

    public SimpleName className;
    public String archetype;
    public String templateId;
    public List<TemplateField> fields;
    public boolean isCompositionBase;
    public boolean isEnumValueSet;

    public TemplateClass(SimpleName className)
    {
        this.className = className;
        this.fields = new ArrayList<>();

        this.isCompositionBase =  this.className.toString().endsWith("Composition");
        this.isEnumValueSet = false;
    }

    public String toString(){
        StringBuilder output =  new StringBuilder("Class: " + this.className + "\nFields: ");

        for(TemplateField field: this.fields)
        {
            output.append(field.toString() + "\n");
        }

        return output.toString();
    }
}

