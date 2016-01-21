package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.Constants;
import hudson.Util;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
 *
 */
public class CommonParsingUtils {
  private static final Integer LIMIT_TXT_FILES = 5;
  private static final String EXT_TEXT_FILE = ".txt";
  private static final String EXT_ZIP_FILE = ".zip";
  private static final String JUNIT_PREFIX = "TEST-*";
  public static final String JUNIT_SUFFIX = "/*.xml";

  /**
   * 
   * @param testResults
   * @param startTime
   * @return
   */
  public static List<AutomationTestResult> toAutomationTestResults(List<TestResult> testResults, Date startTime, Date completedTime) throws Exception{
    HashMap<String, AutomationTestResult> automationTestResultMap = new HashMap<>();

    AutomationTestResult automationTestResult = null;
    AutomationTestLog automationTestLog = null;
    
    for(TestResult testResult: testResults){
      for (SuiteResult suite : testResult.getSuites()) {
        if (suite.getCases() == null) {
          continue;
        } else {
          for (CaseResult caseResult : suite.getCases()) {
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
            
            automationTestLog = new AutomationTestLog();
            automationTestLog.setDescription(caseResult.getName());
            automationTestLog.setExpectedResult(caseResult.getName());
            automationTestLog.setStatus(caseResult.getStatus().toString());

            automationTestResult.addTestLog(automationTestLog);
            if (caseResult.isFailed()) {
              AutomationAttachment attachment = new AutomationAttachment();
              attachment.setName(caseResult.getName().concat(EXT_TEXT_FILE));
              attachment.setData(caseResult.getErrorStackTrace());
              automationTestResult.getAttachments().add(attachment);
            }
          }
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
        zipFile = File.createTempFile(automationTestResult.getName(), EXT_ZIP_FILE);
        zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

        Map<String, Integer> attachmentNames = new HashMap<>();
        for (int i = 0; i < totalAttachments; i++) {
          AutomationAttachment attachment = automationTestResult.getAttachments().get(i);
          String attachmentName = attachment.getName();
          if (attachmentNames.containsKey(attachment.getName())) {
            Integer count = attachmentNames.get(attachment.getName());
            attachmentName = attachmentName.replace(EXT_TEXT_FILE, "_" + count + EXT_TEXT_FILE);
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
        zipFileLength = (int) zipFile.length();
        zipFileBytes = new byte[zipFileLength];
        bufferedInputStream.read(zipFileBytes, 0, zipFileLength);
        bufferedInputStream.close();
        AutomationAttachment attachment = new AutomationAttachment();
        attachment.setData(Base64.encodeBase64String(zipFileBytes));
        attachment.setContentType(Constants.CONTENT_TYPE_ZIP);
        attachment.setName(automationTestResult.getName() + EXT_ZIP_FILE);
        // add zip file
        automationTestResult.setAttachments(Arrays.asList(attachment));
        // remove zip tmp file
        zipFile.delete();
      } else {
        for (int i = 0; i < totalAttachments; i++) {
          AutomationAttachment attachment = automationTestResult.getAttachments().get(i);
          attachment.setContentType(Constants.CONTENT_TYPE_TEXT);
          attachment.setData(Base64.encodeBase64String(attachment.getData().getBytes()));
        }
      }
    }
    return new ArrayList<>(automationTestResultMap.values());
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
