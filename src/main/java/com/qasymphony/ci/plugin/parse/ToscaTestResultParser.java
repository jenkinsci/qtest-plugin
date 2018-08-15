package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.Constants;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.ExternalTool;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import com.qasymphony.ci.plugin.utils.XMLFileUtils;
import hudson.Util;
import hudson.model.Run;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * scan and parse Tosca test results
 *
 * @author tamvo
 * @version 1.0
 * @since 15/8/2018 1:15 PM tamvo $
 */
public class ToscaTestResultParser {
  private static final Logger LOG = Logger.getLogger(ToscaTestResultParser.class.getName());

  /**
   * @param request request
   * @return list of {@link AutomationTestResult}
   * @throws Exception Exception
   */

  public static List<AutomationTestResult> parse(ParseRequest request) throws Exception {
    PrintStream logger = request.getListener().getLogger();
    LoggerUtils.formatInfo(logger, "Scan Tosca test results files and parse the results step.");
    ExternalTool toscaIntegrationConfig = request.getToscaIntegration();
    if ( toscaIntegrationConfig == null) {
      throw new Exception("Tosca integration config doesn't exist. Ignore this step");
    }
    String pathToResults = toscaIntegrationConfig.getPathToResults();
    String pattern =CommonParsingUtils.getResultFilesPattern(pathToResults);
    List<String> resultFiles = CommonParsingUtils.scanTestResultFile(pathToResults, pattern);

    for (String resultFile : resultFiles) {
      LOG.info("Parsing result file: " + resultFile);
      File file = new File(pathToResults, resultFile);
      Document doc = XMLFileUtils.readXMLFile(file);
      doc.getDocumentElement().normalize();
      NodeList testCaseNodes = doc.getElementsByTagName("executionEntry");
      for (int i = 0; i < testCaseNodes.getLength(); i++) {
        Node testCaseNode = testCaseNodes.item(i);
        // make sure it's element node.
        if (testCaseNode.getNodeType() == Node.ELEMENT_NODE) {
          Element testCaseElement = (Element) testCaseNode;
          String testCaseName = testCaseElement.getElementsByTagName("name").item(0).getTextContent();
          String startTime = testCaseElement.getElementsByTagName("startTime").item(0).getTextContent();
          String endTime = testCaseElement.getElementsByTagName("endTime").item(0).getTextContent();
          LOG.info("Getting test case info: " + testCaseName);
          NodeList testStepNodes = testCaseElement.getElementsByTagName("testStepLog");
          for (int j = 0; j < testStepNodes.getLength(); j++) {
            Node testStepNode = testStepNodes.item(j);
            if (testStepNode.getNodeType() == Node.ELEMENT_NODE) {
              Element testStepElement = (Element) testStepNode;
              String testStepName = testStepElement.getElementsByTagName("name").item(0).getTextContent();
              LOG.info("Getting test steps info: " + testStepName);
              String testStepStatus = testStepElement.getElementsByTagName("result").item(0).getTextContent();
            }
          }
        }
      }
    }

//    Run<?,?> build = request.getBuild();

//    //otherwise auto scan test result
//    String basedDir = request.getWorkSpace().toURI().getPath();
//    List<String> resultFolders = CommonParsingUtils.scanJunitTestResultFolder(basedDir);
//    LOG.info("Scanning junit test result in dir:" + basedDir + String.format(".Found: %s dirs, %s", resultFolders.size(), resultFolders));
    List<AutomationTestResult> result = new LinkedList<>();
    return result;
  }

}
