package com.dataflow.generation;

import com.dataflow.exportable.MappingInfo;
import com.dataflow.exportable.TemplateClass;
import com.dataflow.exportable.TemplateField;
import com.dataflow.generation.resources.readers.OPTReader;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompositionGenerator extends JavaParserComponent {

    private static String OPT_BASE_PATH = "/fhir-bridge/src/main/java/org/ehrbase/fhirbridge/ehr/opt/";
    private static String PATH_ANNOTATION_NAME = "Path";

    public List<TemplateClass> templateClasses;
    public MappingInfo mappingInfo;

    public CompositionGenerator(String compositionName) throws Exception {

        String compositionDir = getCompositionDir(compositionName);

        if (compositionDir == null) {
            throw new Exception("Could not extract exportable dir for " + compositionName);
        }

        List<CompilationUnit> cus = createCompilationUnitsRecursively(compositionDir);
        cus.addAll(createCompilationUnitsRecursively(OPT_BASE_PATH + "shareddefinition"));
        this.templateClasses = new ArrayList<>();

        for (CompilationUnit cu : cus) {
            this.templateClasses.add(this.create(cu));
        }
    }

    private static String getCompositionDir(String compositionName) {
        File f = new File(OPT_BASE_PATH);
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File fObj : f.listFiles()) {
                    if (fObj.isDirectory()) {
                        List<String> files = JavaParserComponent.getFilesRecursively(fObj);

                        if (files.stream().anyMatch(file -> file.endsWith("/" + compositionName + ".java"))) {
                            return fObj.toString();
                        }
                    }
                }
            }
        }

        return null;
    }

    private TemplateClass create(CompilationUnit cu) {

        TemplateClass templateClass = new TemplateClass(cu.getTypes().get(0).getName());

        if(cu.getTypes().get(0).getAnnotationByName("Archetype").isPresent()) {

            String archetype = cu.getTypes().get(0).getAnnotationByName("Archetype").get().toString();

            if(archetype.startsWith("@"))
            {
                Pattern pattern = Pattern.compile("@Archetype\\(\"(.*?)\"\\)");
                Matcher matcher = pattern.matcher(archetype);
                if (matcher.find())
                {
                    templateClass.archetype =matcher.group(1);
                }
            }
        }

        if(cu.getTypes().get(0).getAnnotationByName("Template").isPresent()) {

            String template = cu.getTypes().get(0).getAnnotationByName("Template").get().toString();

            if(template.startsWith("@"))
            {
                Pattern pattern = Pattern.compile("@Template\\(\"(.*?)\"\\)");
                Matcher matcher = pattern.matcher(template);
                if (matcher.find())
                {
                    templateClass.templateId =matcher.group(1);
                    this.mappingInfo = OPTReader.exportTemplateInformation(templateClass.templateId);
                }
            }
        }

        if (cu.getTypes().get(0).toEnumDeclaration().isPresent()) {
            templateClass.isEnumValueSet = true;
        }


        executeForEachChildNode(cu, this::addField, templateClass);
        executeForEachChildNode(cu, this::createMethod, templateClass);

        return templateClass;
    }

    private void executeForEachChildNode(CompilationUnit cu, BiConsumer<Node, TemplateClass> consumer, TemplateClass templateClass) {
        for (TypeDeclaration<?> type : cu.getTypes()) {
            List<Node> childNodes = type.getChildNodes();
            for (Node node : childNodes) {
                consumer.accept(node, templateClass);
            }
        }
    }

    private void addField(Node node, TemplateClass templateClass) {
        if (node instanceof FieldDeclaration) {
            FieldDeclaration fd = (FieldDeclaration) node;

            if (fd.getAnnotationByName(PATH_ANNOTATION_NAME).isPresent() && !fd.getVariables().isEmpty()) {

                TemplateField field = new TemplateField(fd.getVariables().get(0).getName(), fd.getCommonType().toString(),
                        fd.getAnnotationByName(PATH_ANNOTATION_NAME).get().toString());
                templateClass.fields.add(field);
            }

        }
    }

    private void createMethod(Node node, TemplateClass templateClass) {
        if (node instanceof CallableDeclaration) {
            if (node instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) node;
                String methodName = md.getName().asString();

                if (methodName.startsWith("set") || methodName.startsWith("get")) {
                    Optional<TemplateField> field = templateClass.fields.stream().filter(f ->
                            f.fieldName.asString().equals(Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4))
                    ).findFirst();

                    if(methodName.startsWith("set"))
                    {
                        field.ifPresent(templateField -> templateField.setterName = md.getName());
                    } else {
                        field.ifPresent(templateField -> templateField.getterName = md.getName());
                    }
                }
            }
        }
    }

}
