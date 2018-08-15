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
    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder();
    return dBuilder.parse(xmlFile);
  }
}