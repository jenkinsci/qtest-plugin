package com.qasymphony.ci.plugin.utils;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * @author tamvo
 * @version 8/15/2018 2:09 PM tamvo $
 * @since 1.0
 */
public class XMLFileUtils {
  public static Document readXMLFile(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // Harden against XXE (CWE-611): these result files come from the build workspace, which is
    // not a trusted source. Disallowing DOCTYPE declarations entirely is the OWASP-recommended
    // primary defense; the external-entity/DTD settings below are defense-in-depth for parsers
    // that don't honor disallow-doctype-decl.
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    DocumentBuilder dBuilder = factory.newDocumentBuilder();
    return dBuilder.parse(xmlFile);
  }
}