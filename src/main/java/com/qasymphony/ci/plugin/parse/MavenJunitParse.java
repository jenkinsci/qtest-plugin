/**
 *
 */
package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationAttachment;
import com.qasymphony.ci.plugin.model.AutomationTestLog;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.CaseResult.Status;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author anpham
 */
public class MavenJunitParse implements TestResultParse {
  public static final String TEST_RESULT_LOCATIONS = "target/surefire-reports/*.xml";
  private static final Integer LIMIT_TXT_FILES = 5;
  @SuppressWarnings("rawtypes")
  private AbstractBuild build;
  private Launcher launcher;
  private BuildListener listener;

  @SuppressWarnings("rawtypes")
  public MavenJunitParse(AbstractBuild build, Launcher launcher, BuildListener listener) {
    this.build = build;
    this.launcher = launcher;
    this.listener = listener;
  }

  public List<AutomationTestResult> parse(String testResultLocation) throws Exception {
    JUnitParser jUnitParser = new JUnitParser(true);
    HashMap<String, AutomationTestResult> automationTestResultMap = new HashMap<>();

    AutomationTestResult automationTestResult = null;
    AutomationTestLog automationTestLog = null;

    int testlogOrder = 1;
    Date current = new Date();

    TestResult testResult = jUnitParser.parseResult(testResultLocation, build, build.getWorkspace(), launcher, listener);
    AutomationAttachment attachment = null;

    for (SuiteResult suite : testResult.getSuites()) {
      if (suite.getCases() == null) {
        continue;
      } else {
        for (CaseResult caseResult : suite.getCases()) {
          if (automationTestResultMap.containsKey(caseResult.getClassName())) {
            automationTestResult = automationTestResultMap.get(caseResult.getClassName());
            if (caseResult.isFailed()) {
              automationTestResult.setStatus(Status.FAILED.toString());
            }
          } else {
            automationTestResult = new AutomationTestResult();
            automationTestResult.setName(caseResult.getClassName());
            automationTestResult.setAutomationContent(caseResult.getClassName());
            automationTestResult.setExecutedEndDate(current);
            automationTestResult.setExecutedStartDate(current);
            automationTestResult.setStatus(caseResult.isPassed() ? Status.PASSED.toString() : Status.FAILED.toString());
            automationTestResult.setTestLogs(new ArrayList<AutomationTestLog>());
            automationTestResult.setAttachments(new ArrayList<AutomationAttachment>());

            automationTestResultMap.put(caseResult.getClassName(), automationTestResult);
          }
          automationTestLog = new AutomationTestLog();
          automationTestLog.setDescription(caseResult.getName());
          automationTestLog.setExpectedResult(caseResult.getName());
          automationTestLog.setOrder(testlogOrder);
          automationTestLog.setStatus(caseResult.getStatus().toString());

          automationTestResult.addTestLog(automationTestLog);
          if (caseResult.isFailed()) {
            attachment = new AutomationAttachment();
            attachment.setName(caseResult.getName().concat(".txt"));
            //attachment.setContentType("text/plain");
            //attachment.setData(Base64.encodeBase64String(caseResult.getErrorStackTrace().getBytes()));
            attachment.setData(caseResult.getErrorStackTrace());
            automationTestResult.getAttachments().add(attachment);
          }

          testlogOrder++;
        }
      }
    }

    Iterator<String> keys = automationTestResultMap.keySet().iterator();
    String key = null;
    int totalAttachments = 0;
    File zipFile = null;
    int zipFileLength = 0;
    byte[] zipFileBytes = null;
    ZipOutputStream zipOutputStream = null;

    while (keys.hasNext()) {
      key = keys.next();
      automationTestResult = automationTestResultMap.get(key);
      totalAttachments = automationTestResult.getAttachments().size();

      if (totalAttachments > LIMIT_TXT_FILES) {
        zipFile = File.createTempFile(automationTestResult.getName(), ".zip");
        zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

        for (int i = 0; i < totalAttachments; i++) {
          attachment = automationTestResult.getAttachments().get(i);

          zipOutputStream.putNextEntry(new ZipEntry(attachment.getName()));
          zipOutputStream.write(attachment.getData().getBytes());

          zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
        //get zipFile stream
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(zipFile));
        zipFileLength = (int) zipFile.length();
        zipFileBytes = new byte[zipFileLength];
        bufferedInputStream.read(zipFileBytes, 0, zipFileLength);
        bufferedInputStream.close();

        automationTestResult.setAttachments(new ArrayList<AutomationAttachment>());

        attachment = new AutomationAttachment();
        attachment.setData(Base64.encodeBase64String(zipFileBytes));
        attachment.setContentType("application/zip");
        attachment.setName(zipFile.getName());
        // add zip file
        automationTestResult.getAttachments().add(attachment);

      } else {
        for (int i = 0; i < totalAttachments; i++) {
          attachment = automationTestResult.getAttachments().get(i);
          attachment.setContentType("text/plain");
          attachment.setData(Base64.encodeBase64String(attachment.getData().getBytes()));
        }
      }
    }

    return new ArrayList<>(automationTestResultMap.values());
  }

  @Override
  public List<AutomationTestResult> parse() throws Exception {
    return parse(TEST_RESULT_LOCATIONS);
  }
}
