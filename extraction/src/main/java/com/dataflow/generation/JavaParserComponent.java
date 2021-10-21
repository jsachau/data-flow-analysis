package com.dataflow.generation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class JavaParserComponent {

    protected static List<String> getFilesRecursively(File dir) {
        List<String> ls = new ArrayList<String>();
        try {
            for (File fObj : dir.listFiles()) {
                if (fObj.isDirectory()) {
                    ls.addAll(getFilesRecursively(fObj));
                } else if (fObj.toString().endsWith(".java")) {
                    ls.add(String.valueOf(fObj));
                }
            }

        } catch (NullPointerException e) {
            return ls;
        }

        return ls;
    }

    private static List<String> getListOfFiles(String fullPathDir) {
        List<String> ls = new ArrayList<String>();
        File f = new File(fullPathDir);
        if (f.exists()) {
            if (f.isDirectory()) {
                ls.addAll(getFilesRecursively(f));
            }
        } else if (fullPathDir.endsWith(".java")) {
            ls.add(fullPathDir);
        }
        return ls;
    }

    static List<CompilationUnit> createCompilationUnitsRecursively(String inputDirectory) {
        List<String> ls = getListOfFiles(inputDirectory);
        List<CompilationUnit> cds = new ArrayList<>();

        for (String file : ls) {
            cds.add(createCompilationUnit(file));
        }

        return cds;
    }

    static SymbolResolver getSymbolResolver() throws IOException{
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver(false));
        typeSolver.add(new JavaParserTypeSolver(Configuration.FHIR_BRIDGE_BASE_DIR));
        typeSolver.add(new JarTypeSolver("/fhir-bridge-dependencies/org.hl7.fhir.r4-5.1.0.jar"));
        typeSolver.add(new JarTypeSolver("/fhir-bridge-dependencies/archie-0.3.16.jar"));

        return new JavaSymbolSolver(typeSolver);
    }

    static CompilationUnit createCompilationUnit(String inputClass) {
        CompilationUnit cu = null;
        try (FileInputStream in = new FileInputStream(inputClass)) {
            StaticJavaParser.getConfiguration().setSymbolResolver(getSymbolResolver());
            cu = StaticJavaParser.parse(in);
        } catch (FileNotFoundException e) {
            throw new UncheckedExecutionException("Could not parse class at location: " + inputClass, e);
        } catch (IOException e) {
            throw new UncheckedExecutionException("Unable to close input stream for: " + inputClass, e);
        }
        return cu;
    }

}
