package com.qasymphony.ci.plugin.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Regression test hardening XMLFileUtils.readXMLFile against XXE (CWE-611). This is fed
 * untrusted build-workspace XML by both Tosca result parsers, with no prior hardening.
 */
public class XMLFileUtilsTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void rejectsExternalEntityInjection() throws Exception {
    File secret = tempFolder.newFile("secret.txt");
    Files.write(secret.toPath(), "top-secret-value".getBytes());

    File xxePayload = tempFolder.newFile("xxe.xml");
    try (FileWriter writer = new FileWriter(xxePayload)) {
      writer.write("<?xml version=\"1.0\"?>\n"
              + "<!DOCTYPE root [\n"
              + "  <!ENTITY xxe SYSTEM \"file://" + secret.getAbsolutePath() + "\">\n"
              + "]>\n"
              + "<root>&xxe;</root>\n");
    }

    try {
      XMLFileUtils.readXMLFile(xxePayload);
      fail("Expected the DOCTYPE declaration to be rejected");
    } catch (org.xml.sax.SAXParseException expected) {
      // disallow-doctype-decl correctly rejected the document before the external entity
      // could be resolved
    }
  }

  @Test
  public void stillParsesOrdinaryXml() throws Exception {
    File plainXml = tempFolder.newFile("plain.xml");
    try (FileWriter writer = new FileWriter(plainXml)) {
      writer.write("<?xml version=\"1.0\"?><root><child>value</child></root>");
    }

    Document doc = XMLFileUtils.readXMLFile(plainXml);
    assertEquals("root", doc.getDocumentElement().getTagName());
    assertEquals("value", doc.getElementsByTagName("child").item(0).getTextContent());
  }
}
