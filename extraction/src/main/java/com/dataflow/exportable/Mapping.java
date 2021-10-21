package com.dataflow.exportable;

import com.dataflow.exportable.TemplateClass;

import java.util.List;

public class Mapping {

    public String name;
    public List<TemplateClass> templateClasses;
    public MappingInfo info;


    public Mapping(String name, List<TemplateClass> templateClasses, MappingInfo info)
    {
        this.name = name;
        this.templateClasses = templateClasses;
        this.info = info;
    }
}
