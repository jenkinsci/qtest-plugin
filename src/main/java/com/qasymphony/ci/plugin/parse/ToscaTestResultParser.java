package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.Constants;
import com.qasymphony.ci.plugin.model.AutomationAttachment;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.AutomationTestStepLog;
import com.qasymphony.ci.plugin.model.ExternalTool;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import com.qasymphony.ci.plugin.utils.XMLFileUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
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
    Map<String, AutomationTestResult> map = new HashMap<>();
    int currentTestLogOrder = 1;
    for (String resultFile : resultFiles) {
      LOG.info("Parsing result file: " + resultFile);
      File file = new File(pathToResults, resultFile);
      Document doc = XMLFileUtils.readXMLFile(file);
      doc.getDocumentElement().normalize();
      NodeList testCaseNodes = doc.getElementsByTagName("executionEntry");
      if (testCaseNodes.getLength() <= 0) {
        throw new Exception("Tosca parser cannot find test cases");
      }
      for (int i = 0; i < testCaseNodes.getLength(); i++) {
        Node testCaseNode = testCaseNodes.item(i);
        AutomationTestResult testLog = buildTestCaseLog(testCaseNode, request.getOverwriteExistingTestSteps(), currentTestLogOrder++, logger);
        String testCaseName = testLog.getName();
        if (!map.containsKey(testCaseName)) {
          map.put(testCaseName, testLog);
        }
      }
    }
    map = CommonParsingUtils.processAttachment(map);
    return new ArrayList<>(map.values());
  }

  private static AutomationTestResult buildTestCaseLog(Node testCaseNode, boolean overwriteExistingTestSteps, int currentTestLogOrder, PrintStream logger) {
    AutomationTestResult testLog = null;
    // make sure it's element node.
    if (testCaseNode.getNodeType() == Node.ELEMENT_NODE) {
      Element testCaseElement = (Element) testCaseNode;
      String testCaseName = testCaseElement.getElementsByTagName("name").item(0).getTextContent();
      String startTime = testCaseElement.getElementsByTagName("startTime").item(0).getTextContent();
      String endTime = testCaseElement.getElementsByTagName("endTime").item(0).getTextContent();
      Date startDate;
      Date endDate;
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX");
        startDate = dateFormat.parse(startTime);
        endDate = dateFormat.parse(endTime);
      } catch (Exception e) {
        startDate = new Date();
        endDate = new Date();
      }
      LoggerUtils.formatInfo(logger, "Getting test case info: " + testCaseName);
      NodeList testStepNodes = testCaseElement.getElementsByTagName("testStepLog");

      int totalFailedTestSteps = 0;
      int totalSkippedTestSteps = 0;
      int totalTestSteps = 0;

      int currentTestStepOrder = 0;
      List<AutomationTestStepLog> testStepLogs = new ArrayList<>();
      List<AutomationAttachment> attachments = new ArrayList<>();
      for (int j = 0; j < testStepNodes.getLength(); j++) {
        Node testStepNode = testStepNodes.item(j);
        Element testStepElement = (Element) testStepNode;
        if (testStepNode.getNodeType() == Node.ELEMENT_NODE) {
          AutomationTestStepLog testStepLog = buildTestStepLog(testStepElement, currentTestStepOrder++);
          totalTestSteps += 1;
          String testStepStatus = testStepLog.getStatus();
          if (Constants.TestResultStatus.FAILED.equalsIgnoreCase(testStepStatus) ||
                  Constants.TestResultStatus.FAIL.equalsIgnoreCase(testStepStatus) ||
                  Constants.TestResultStatus.ERROR.equalsIgnoreCase(testStepStatus)) {
            totalFailedTestSteps += 1;
            AutomationAttachment attachment = buildAttachments(testStepElement, testStepLog.getExpectedResult());
            attachments.add(attachment);
          }

          if (Constants.TestResultStatus.SKIP.equalsIgnoreCase(testStepStatus) ||
                  Constants.TestResultStatus.SKIPPED.equalsIgnoreCase(testStepStatus)) {
            totalSkippedTestSteps += 1;
          }

          if (overwriteExistingTestSteps) {
            testStepLogs.add(testStepLog);
          }
        }
      }
        testLog = new AutomationTestResult();
        testLog.setOrder(currentTestLogOrder);
        testLog.setAutomationContent(testCaseName);
        testLog.setExecutedStartDate(startDate);
        testLog.setExecutedEndDate(endDate);
        testLog.setTestLogs(testStepLogs);
        testLog.setAttachments(attachments);
        testLog.setStatus(Constants.TestResultStatus.PASS);
        if (totalFailedTestSteps >= 1) {
          testLog.setStatus(Constants.TestResultStatus.FAIL);
        } else if (totalSkippedTestSteps == totalTestSteps) {
          testLog.setStatus(Constants.TestResultStatus.SKIP);
        }
    }
    return testLog;
  }

  private static AutomationTestStepLog buildTestStepLog(Element testStepElement, int testStepOrder) {
    String testStepName = testStepElement.getElementsByTagName("name").item(0).getTextContent();
    LOG.info("Getting test steps info: " + testStepName);
    String testStepStatus = testStepElement.getElementsByTagName("result").item(0).getTextContent();
    AutomationTestStepLog testStepsLog = new AutomationTestStepLog();
    testStepsLog.setStatus(testStepStatus.toUpperCase());
    testStepsLog.setExpectedResult(testStepName);
    testStepsLog.setDescription(testStepName);
    testStepsLog.setOrder(testStepOrder);
    return testStepsLog;
  }

  private static AutomationAttachment buildAttachments(Element testStepElement, String testStepName) {
    String testStepErrorMessage = testStepElement.getElementsByTagName("detail").item(0).getTextContent();
    if (StringUtils.isEmpty(testStepErrorMessage)) {
      testStepErrorMessage = testStepElement.getElementsByTagName("description").item(0).getTextContent();
    }
    AutomationAttachment attachment =  new AutomationAttachment();
    attachment.setName(testStepName.concat(Constants.Extension.TEXT_FILE));
    attachment.setContentType(Constants.CONTENT_TYPE_TEXT);
    attachment.setData(testStepErrorMessage);
    return attachment;
  }
}
