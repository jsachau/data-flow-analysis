package com.dataflow.generation.resources.readers;

import com.dataflow.exportable.MappingInfo;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OPTReader {

    private static final String OPT_BASE_PATH = "/fhir-bridge/src/main/resources/opt/";
    private static final String TEMPLATE_ID_XPATH = "/*[local-name()='template']/*[local-name()='template_id']/*[local-name()='value']";
    private static final String DESCRIPTION_XPATH = "/*[local-name()='template']/*[local-name()='description']/*[local-name()='details']/*[local-name()='use']";
    private static final String AUTHOR_XPATH = "/*[local-name()='template']/*[local-name()='description']/*[local-name()='original_author' and lower-case(@id)='name']";
    private static final String ORGANISATION_XPATH = "/*[local-name()='template']/*[local-name()='description']/*[local-name()='original_author' and lower-case(@id)='organisation']";
    private static final String DATE_XPATH = "/*[local-name()='template']/*[local-name()='description']/*[local-name()='original_author' and lower-case(@id)='date']";
    private static final String EMAIL_XPATH = "/*[local-name()='template']/*[local-name()='description']/*[local-name()='original_author' and lower-case(@id)='email']";

    public static MappingInfo exportTemplateInformation(String templateId) {
        List<String> ls = new ArrayList<String>();
        File f = new File(OPT_BASE_PATH);
        MappingInfo info = new MappingInfo();

        for (File fObj : f.listFiles()) {
            if (fObj.toString().endsWith(".opt")) {
                try {
                    Document doc = readXml(new FileInputStream(fObj.getAbsolutePath()));
                    processFilteredXml(doc, TEMPLATE_ID_XPATH, (id) -> {
                        if(id.equals(templateId)) {
                            info.templateId = id;

                            processFilteredXml(doc, DESCRIPTION_XPATH, (description) -> {
                               info.description = description;
                            });

                            processFilteredXml(doc, AUTHOR_XPATH, (author) -> {
                                info.author = author;
                            });

                            processFilteredXml(doc, ORGANISATION_XPATH, (organisation) -> {
                                info.organisation = organisation;
                            });

                            processFilteredXml(doc, DATE_XPATH, (date) -> {
                                info.date = date;
                            });

                            processFilteredXml(doc, EMAIL_XPATH, (email) -> {
                                info.email = email;
                            });
                        }
                    });

                    if(info.templateId != null) break;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return info;
    }

    private static void processFilteredXml(Document doc, String xpath, Consumer<String> process) {
        process.accept(filterNodesByXPath(doc, xpath));
    }

    public static Document readXml(InputStream xmlin) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(xmlin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String filterNodesByXPath(Document doc, String xpathExpr) {
        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            String eval = xp.evaluate(xpathExpr, doc);
            return (String) eval;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
