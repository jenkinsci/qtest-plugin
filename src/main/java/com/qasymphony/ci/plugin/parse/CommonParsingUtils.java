package com.qasymphony.ci.plugin.parse;

import static com.qasymphony.ci.plugin.Constants.*;

import hudson.Util;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;

import com.qasymphony.ci.plugin.model.AutomationAttachment;
import com.qasymphony.ci.plugin.model.AutomationTestLog;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * @author anpham
 */
public class CommonParsingUtils {

  /**
   * @param useTestMethodAsTestCase
   * @param testResults
   * @param startTime
   * @param completedTime
   * @return
   * @throws Exception
   */
  public static List<AutomationTestResult> toAutomationTestResults(boolean useTestMethodAsTestCase, List<TestResult> testResults, Date startTime, Date completedTime)
    throws Exception {
    if (useTestMethodAsTestCase) {
      return useTestMethodAsTestCase(testResults, startTime, completedTime);
    } else {
      return useClassNameAsTestCase(testResults, startTime, completedTime);
    }
  }

  private static List<AutomationTestResult> useTestMethodAsTestCase(List<TestResult> testResults, Date startTime, Date completedTime)
    throws Exception {
    HashMap<String, AutomationTestResult> automationTestResultMap = new HashMap<>();

    for (TestResult testResult : testResults) {
      for (SuiteResult suite : testResult.getSuites()) {
        if (suite.getCases() == null) {
          continue;
        } else {
          for (CaseResult caseResult : suite.getCases()) {
            String automationContent = caseResult.getClassName() + "#" + caseResult.getName();

            if (!automationTestResultMap.containsKey(automationContent)) {
              AutomationTestResult automationTestResult = new AutomationTestResult();
              automationTestResult.setName(automationContent);
              automationTestResult.setAutomationContent(automationContent);
              automationTestResult.setExecutedEndDate(completedTime);
              automationTestResult.setExecutedStartDate(startTime);
              automationTestResult.setAttachments(new ArrayList<AutomationAttachment>());

              AutomationTestLog automationTestLog = new AutomationTestLog(caseResult);
              automationTestResult.addTestLog(automationTestLog);

              if (caseResult.isFailed()) {
                AutomationAttachment attachment = new AutomationAttachment(caseResult);
                attachment.setData(Base64.encodeBase64String(attachment.getData().getBytes()));
                automationTestResult.getAttachments().add(attachment);
              }

              automationTestResultMap.put(automationContent, automationTestResult);
            }
          }
        }
      }
    }

    return new ArrayList<>(automationTestResultMap.values());
  }

  private static List<AutomationTestResult> useClassNameAsTestCase(List<TestResult> testResults, Date startTime, Date completedTime)
    throws Exception {
    Map<String, AutomationTestResult> automationTestResultMap = new HashMap<>();

    for (TestResult testResult : testResults) {
      for (SuiteResult suite : testResult.getSuites()) {
        if (suite.getCases() == null) {
          continue;
        } else {
          for (CaseResult caseResult : suite.getCases()) {
            AutomationTestResult automationTestResult = null;
            if (automationTestResultMap.containsKey(caseResult.getClassName())) {
              automationTestResult = automationTestResultMap.get(caseResult.getClassName());
            } else {
              automationTestResult = new AutomationTestResult();
              automationTestResult.setName(caseResult.getClassName());
              automationTestResult.setAutomationContent(caseResult.getClassName());
              automationTestResult.setExecutedEndDate(completedTime);
              automationTestResult.setExecutedStartDate(startTime);
              automationTestResult.setAttachments(new ArrayList<AutomationAttachment>());
              automationTestResultMap.put(caseResult.getClassName(), automationTestResult);
            }

            AutomationTestLog automationTestLog = new AutomationTestLog(caseResult);
            automationTestResult.addTestLog(automationTestLog);
            if (caseResult.isFailed()) {
              AutomationAttachment attachment = new AutomationAttachment(caseResult);
              automationTestResult.getAttachments().add(attachment);
            }
          }
        }
      }
    }
    automationTestResultMap = processAttachment(automationTestResultMap);
    return new ArrayList<>(automationTestResultMap.values());
  }

  /**
   * Process attachment
   *
   * @param automationTestResultMap
   * @return
   * @throws Exception
   */
  private static Map<String, AutomationTestResult> processAttachment(Map<String, AutomationTestResult> automationTestResultMap)
    throws Exception {

    Iterator<String> keys = automationTestResultMap.keySet().iterator();

    while (keys.hasNext()) {
      String key = keys.next();
      AutomationTestResult automationTestResult = automationTestResultMap.get(key);
      int totalAttachments = automationTestResult.getAttachments().size();
      if (totalAttachments > LIMIT_TXT_FILES) {
        File zipFile = File.createTempFile(automationTestResult.getName(), Extension.ZIP_FILE);
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

        Map<String, Integer> attachmentNames = new HashMap<>();
        for (int i = 0; i < totalAttachments; i++) {
          AutomationAttachment attachment = automationTestResult.getAttachments().get(i);
          String attachmentName = attachment.getName();
          if (attachmentNames.containsKey(attachment.getName())) {
            Integer count = attachmentNames.get(attachment.getName());
            attachmentName = attachmentName.replace(Extension.TEXT_FILE, "_" + count + Extension.TEXT_FILE);
            attachmentNames.put(attachment.getName(), ++count);
          } else {
            attachmentNames.put(attachment.getName(), 1);
          }
          zipOutputStream.putNextEntry(new ZipEntry(attachmentName));
          zipOutputStream.write(attachment.getData().getBytes());

          zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
        //get zipFile stream
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(zipFile));
        int zipFileLength = (int) zipFile.length();
        byte[] zipFileBytes = new byte[zipFileLength];
        bufferedInputStream.read(zipFileBytes, 0, zipFileLength);
        bufferedInputStream.close();
        AutomationAttachment attachment = new AutomationAttachment();
        attachment.setData(Base64.encodeBase64String(zipFileBytes));
        attachment.setContentType(CONTENT_TYPE_ZIP);
        attachment.setName(automationTestResult.getName() + Extension.ZIP_FILE);
        // add zip file
        automationTestResult.setAttachments(Arrays.asList(attachment));
        // remove zip tmp file
        zipFile.delete();
      } else {
        for (int i = 0; i < totalAttachments; i++) {
          AutomationAttachment attachment = automationTestResult.getAttachments().get(i);
          attachment.setContentType(CONTENT_TYPE_TEXT);
          attachment.setData(Base64.encodeBase64String(attachment.getData().getBytes()));
        }
      }
    }
    return automationTestResultMap;
  }

  /**
   * Scan junit test result folder
   *
   * @param basedDir
   * @return
   */
  public static List<String> scanJunitTestResultFolder(String basedDir) {
    File currentBasedDir = new File(basedDir);
    FileSet fs = Util.createFileSet(new File(basedDir), JUNIT_PREFIX);
    DirectoryScanner ds = fs.getDirectoryScanner();
    List<String> resultFolders = new ArrayList<>();
    //if based dir match junit file, we add based dir
    if (ds.getIncludedFiles().length > 0)
      resultFolders.add("");

    for (String notIncludedDirName : ds.getNotIncludedDirectories()) {
      if (!StringUtils.isEmpty(notIncludedDirName)) {
        File dirToScan = new File(currentBasedDir.getPath(), notIncludedDirName);
        FileSet subFileSet = Util.createFileSet(dirToScan, JUNIT_PREFIX);
        DirectoryScanner subDirScanner = subFileSet.getDirectoryScanner();
        if (subDirScanner.getIncludedFiles().length > 0) {
          resultFolders.add(notIncludedDirName);
        }
      }
    }
    return resultFolders;
  }
}
