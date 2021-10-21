package com.dataflow.analysis.dependency;

import com.dataflow.exportable.TemplateField;

public class TemplateFieldVertex extends DependencyVertex {

    private TemplateField field;

    public TemplateFieldVertex(TemplateField field) {
        this.field = field;
    }

    @Override
    public boolean equals(Object obj) {

        if(obj instanceof DependencyVertex && !super.equals(obj)) return false;

        if(obj instanceof TemplateFieldVertex) {
            return this.field.equals(((TemplateFieldVertex) obj).field);
        }

        return super.equals(obj);
    }

    public TemplateField getField() {
        return field;
    }

    @Override
    public String toString() {
        return "Field:" + this.field.fieldName.toString();
    }


}
