package com.dataflow.exportable;

import com.github.javaparser.ast.expr.SimpleName;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateField {

    public SimpleName fieldName;
    public String type;

    public String path;
    public SimpleName setterName;
    public SimpleName getterName;

    public List<TemplateFieldMapping> observationTemplateFieldMappings;

    public TemplateField(SimpleName fieldName, String type, String path)
    {
        this.fieldName = fieldName;
        this.type = type;

        if(path.startsWith("@"))
        {
            Pattern pattern = Pattern.compile("@Path\\(\"(.*?)\"\\)");
            Matcher matcher = pattern.matcher(path);
            if (matcher.find())
            {
                path =matcher.group(1);
            }
        }
        this.path = path;

        this.observationTemplateFieldMappings = new ArrayList<>();
    }

    public String toString()
    {
        return this.fieldName.asString() + ";\n" +
                "Path: " + this.path +";\n";

    }
}
