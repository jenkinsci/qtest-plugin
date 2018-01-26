package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationAttachment;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.AutomationTestStepLog;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Util;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.qasymphony.ci.plugin.Constants.*;

/**
 * @author anpham
 */
public class CommonParsingUtils {

  private CommonParsingUtils() {

  }

  public static List<AutomationTestResult> toAutomationTestResults(ParseRequest request, List<TestResult> testResults, Date startTime)
    throws Exception {
    if (request.getConfiguration().getEachMethodAsTestCase()) {
      return useTestMethodAsTestCase(request, testResults, startTime);
    } else {
      return useClassNameAsTestCase(request, testResults, startTime);
    }
  }

  /**
   * Each method as a testCase in qTest
   *
   * @param request     parse request
   * @param testResults testResults
   * @param startTime   start build time
   * @return
   * @throws Exception
   */
  private static List<AutomationTestResult> useTestMethodAsTestCase(ParseRequest request, List<TestResult> testResults, Date startTime)
    throws Exception {
    Map<String, AutomationTestResult> map = new HashMap<>();
    int currentTestLogOrder = 0;

    for (TestResult testResult : testResults) {
      for (SuiteResult suite : testResult.getSuites()) {
        if (suite.getCases() == null) {
          continue;
        }
        Date startDate = JsonUtils.parseTimestamp(suite.getTimestamp());
        startDate = startDate == null ? startTime : startDate;
        for (CaseResult caseResult : suite.getCases()) {
          String automationContent = caseResult.getClassName() + "#" + caseResult.getName();

          if (!map.containsKey(automationContent)) {
            AutomationTestResult testLog = new AutomationTestResult();
            testLog.setOrder(currentTestLogOrder++);
            testLog.setAutomationContent(automationContent);
            testLog.setExecutedStartDate(startDate);
            testLog.setExecutedEndDate(computeEndTime(startDate, caseResult.getDuration()));
            testLog.addTestStepLog(new AutomationTestStepLog(caseResult), request.getConfiguration());

            if (caseResult.isFailed()) {
              try {
                AutomationAttachment attachment = new AutomationAttachment(caseResult);
                attachment.setData(Base64.encodeBase64String(attachment.getData().getBytes()));
                testLog.addAttachment(attachment);
              } catch (Exception e) {
                LoggerUtils.formatError(request.getListener().getLogger(), "Error while build attachment: %s, %s", e.getMessage(), e.getStackTrace());
              }
            }

            map.put(automationContent, testLog);
          }
        }
      }
    }

    return new ArrayList<>(map.values());
  }

  /**
   * Class name as a testCase in qTest
   *
   * @param request     parse request
   * @param testResults testResults
   * @param startTime   start build time
   * @return
   * @throws Exception
   */
  private static List<AutomationTestResult> useClassNameAsTestCase(ParseRequest request, List<TestResult> testResults, Date startTime)
    throws Exception {
    Map<String, AutomationTestResult> map = new HashMap<>();
    int currentTestLogOrder = 0;

    for (TestResult testResult : testResults) {
      for (SuiteResult suite : testResult.getSuites()) {
        if (suite.getCases() == null) {
          continue;
        }
        Date startDate = JsonUtils.parseTimestamp(suite.getTimestamp());
        startDate = startDate == null ? startTime : startDate;
        for (CaseResult caseResult : suite.getCases()) {
          AutomationTestResult testLog = null;
          if (map.containsKey(caseResult.getClassName())) {
            testLog = map.get(caseResult.getClassName());
          } else {
            testLog = new AutomationTestResult();
            testLog.setOrder(currentTestLogOrder++);
            testLog.setAutomationContent(caseResult.getClassName());
            testLog.setExecutedStartDate(startDate);
            testLog.setExecutedEndDate(computeEndTime(startDate, suite.getDuration()));
            map.put(caseResult.getClassName(), testLog);
          }

          testLog.addTestStepLog(new AutomationTestStepLog(caseResult), request.getConfiguration());
          if (caseResult.isFailed()) {
            testLog.addAttachment(new AutomationAttachment(caseResult));
          }
        }
      }
    }
    map = processAttachment(map);
    return new ArrayList<>(map.values());
  }

  /**
   * Process attachment
   *
   * @param map automationTestResultMap
   */
  private static Map<String, AutomationTestResult> processAttachment(Map<String, AutomationTestResult> map)
    throws Exception {

    Iterator<String> keys = map.keySet().iterator();

    while (keys.hasNext()) {
      String key = keys.next();
      AutomationTestResult testLog = map.get(key);
      int totalAttachments = testLog.getAttachments().size();
      if (totalAttachments > LIMIT_TXT_FILES) {
        File zipFile = File.createTempFile(testLog.getName(), Extension.ZIP_FILE);
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

        Map<String, Integer> attachmentNames = new HashMap<>();
        for (int i = 0; i < totalAttachments; i++) {
          AutomationAttachment attachment = testLog.getAttachments().get(i);
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
        attachment.setName(testLog.getName() + Extension.ZIP_FILE);
        // add zip file
        testLog.setAttachments(Arrays.asList(attachment));
        // remove zip tmp file
        zipFile.delete();
      } else {
        for (int i = 0; i < totalAttachments; i++) {
          AutomationAttachment attachment = testLog.getAttachments().get(i);
          attachment.setContentType(CONTENT_TYPE_TEXT);
          attachment.setData(Base64.encodeBase64String(attachment.getData().getBytes()));
        }
      }
    }
    return map;
  }

  /**
   * Scan junit test result folder
   *
   * @param basedDir basedDir
   * @return a list of folder
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

  /**
   * @param startTime start time
   * @param duration  in second
   * @return
   */
  private static Date computeEndTime(Date startTime, float duration) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startTime);
    calendar.add(Calendar.SECOND, Math.round(duration));
    return calendar.getTime();
  }
}
