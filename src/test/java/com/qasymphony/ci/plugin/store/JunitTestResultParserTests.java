package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.*;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.tasks.junit.CaseResult;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author trongle
 * @version 11/2/2015 11:19 AM trongle $
 * @since 1.0
 */
public class JunitTestResultParserTests extends TestAbstracts {

  public static final class JUnitParserTestAntProject extends Builder implements Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean perform(AbstractBuild<?, ?> build,
      Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
      try {
        File currentBasedDir = new File(build.getWorkspace().toURI());
        List<String> matchDirs = JunitTestResultParser.scanJunitTestResultFolder(currentBasedDir.getPath());
        long current = System.currentTimeMillis();
        for (String dir : matchDirs) {
          File testFolder = new File(currentBasedDir.getPath(), dir);
          testFolder.setLastModified(current);
          for (File file : testFolder.listFiles()) {
            file.setLastModified(current);
          }
        }
        automationTestResultList = JunitTestResultParser.parse(build, launcher, listener);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }
  }

  private FreeStyleProject project;
  private static List<AutomationTestResult> automationTestResultList;

  @Before public void setUp() throws Exception {
  }

  @LocalData
  @Test public void testAntResultProject()
    throws InterruptedException, ExecutionException, TimeoutException, IOException {

    project = j.createFreeStyleProject("ant-project");
    automationTestResultList = null;
    project.getBuildersList().add(new JUnitParserTestAntProject());
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);
    assertEquals("", 1, automationTestResultList.size());
  }

  @LocalData
  @Test public void testGradleResultProject()
    throws InterruptedException, ExecutionException, TimeoutException, IOException {
    project = j.createFreeStyleProject("gradle-project");
    automationTestResultList = null;
    project.getBuildersList().add(new JUnitParserTestAntProject());
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);
    assertEquals("", 20, automationTestResultList.size());
  }

  @LocalData
  @Test public void testSubmitWithAutomationXMLContent()
    throws InterruptedException, ExecutionException, TimeoutException, IOException, SubmittedException {
    project = j.createFreeStyleProject("ant-project");
    automationTestResultList = null;
    project.getBuildersList().add(new JUnitParserTestAntProject());
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);

    String buildNumber = "1";
    String buildPath = "/jobs/AntProjectWithXMLContent/" + buildNumber;
    String projectName = "AntProjectWithXMLContent";
    String apiKey = "9d3971a6-f6d7-4e0b-996c-e2ade023b4e8";
    Long releaseId = 1L;
    Long ciId = 1L;
    Long qTestProjectId = 3L;
    Configuration configuration = new Configuration(ciId, "https://localhost:7443", apiKey, qTestProjectId, projectName,
      releaseId, "releaseName", 0L, "environment", 0L, 0L);
    Map<String, String> headers = OauthProvider.buildHeaders(configuration.getUrl(), configuration.getAppSecretKey(), null);
    AutomationTestResponse response = AutomationTestService.push(buildNumber, buildPath, automationTestResultList, configuration, headers);
    assertNotNull("Result is: ", response);
    assertNotNull("Test suite id is: ", response.getTestSuiteId());
  }

  @Test public void testSubmitLog()
    throws InterruptedException, ExecutionException, TimeoutException, IOException, SubmittedException {
    String buildNumber = "1";
    String buildPath = "/jobs/TestPerformance/" + buildNumber;
    String projectName = "TestPerformance";
    String apiKey = "3c76feb4-b91f-4a53-8643-bd1ce2f01a3e";
    Long releaseId = 1L;
    Long ciId = 3L;
    Long qTestProjectId = 1L;
    Configuration configuration = new Configuration(ciId, "https://localhost:7443", apiKey, qTestProjectId, projectName,
      releaseId, "releaseName", 0L, "environment", 0L, 0L);
    List<AutomationTestResult> results = new ArrayList<>();
    int total = 1000;
    for (int i = 0; i < total; i++) {
      AutomationTestResult automationTestResult = new AutomationTestResult();
      automationTestResult.setName("Test Performance " + i);
      automationTestResult.setAutomationContent(automationTestResult.getName());
      automationTestResult.setStatus(CaseResult.Status.PASSED.toString());
      automationTestResult.setExecutedStartDate(new Date());
      automationTestResult.setExecutedEndDate(new Date());
      results.add(automationTestResult);
      List<AutomationTestLog> testLogs = new ArrayList<>();
      for (int j = 0; j < 10; j++) {
        AutomationTestLog automationTestLog = new AutomationTestLog();
        automationTestLog.setOrder(j);
        automationTestLog.setStatus(CaseResult.Status.PASSED.toString());
        automationTestLog.setDescription("Test Description of " + j + " in class: " + i);
        automationTestLog.setExpectedResult(CaseResult.Status.PASSED.toString());
        testLogs.add(automationTestLog);
      }
      automationTestResult.setTestLogs(testLogs);
    }
    Map<String, String> headers = OauthProvider.buildHeaders(configuration.getUrl(), configuration.getAppSecretKey(), null);
    AutomationTestResponse response = AutomationTestService.push(buildNumber, buildPath, results, configuration, headers);
    assertNotNull("Result is: ", response);
    assertNotNull("Test suite id is: ", response.getTestSuiteId());
  }

  @Test public void testSubmitLogWithAttachment()
    throws InterruptedException, ExecutionException, TimeoutException, IOException, SubmittedException {
    String buildNumber = "1";
    String buildPath = "/jobs/TestPerformance/" + buildNumber;
    String projectName = "TestPerformance";
    String apiKey = "3c76feb4-b91f-4a53-8643-bd1ce2f01a3e";
    Long releaseId = 1L;
    Long ciId = 3L;
    Long qTestProjectId = 1L;
    Configuration configuration = new Configuration(ciId, "https://localhost:7443", apiKey, qTestProjectId, projectName,
      releaseId, "releaseName", 0L, "environment", 0L, 0L);
    List<AutomationTestResult> results = new ArrayList<>();
    long start = System.currentTimeMillis();
    int total = 1000;
    for (int i = 0; i < total; i++) {
      AutomationTestResult automationTestResult = new AutomationTestResult();
      automationTestResult.setName("Test Performance " + i);
      automationTestResult.setAutomationContent(automationTestResult.getName());
      automationTestResult.setStatus(CaseResult.Status.PASSED.toString());
      automationTestResult.setExecutedStartDate(new Date());
      automationTestResult.setExecutedEndDate(new Date());
      results.add(automationTestResult);
      List<AutomationTestLog> testLogs = new ArrayList<>();
      List<AutomationAttachment> automationAttachments = new ArrayList<>();
      for (int j = 0; j < 100; j++) {
        AutomationTestLog automationTestLog = new AutomationTestLog();
        automationTestLog.setOrder(j);
        automationTestLog.setStatus(CaseResult.Status.FAILED.toString());
        automationTestLog.setDescription("Test Description of " + j + " in class: " + i);
        automationTestLog.setExpectedResult(CaseResult.Status.FAILED.toString());
        testLogs.add(automationTestLog);

        AutomationAttachment automationAttachment = new AutomationAttachment();
        automationAttachment.setName(automationTestLog.getDescription() + ".txt");
        automationAttachment.setContentType("text/plain");
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 10; k++)
          sb.append("Test attachment data");
        automationAttachment.setData(sb.toString());
        automationAttachments.add(automationAttachment);
      }
      automationTestResult.setTestLogs(testLogs);
      automationTestResult.setAttachments(automationAttachments);
    }
    Map<String, String> headers = OauthProvider.buildHeaders(configuration.getUrl(), configuration.getAppSecretKey(), null);
    AutomationTestResponse response = AutomationTestService.push(buildNumber, buildPath, results, configuration, headers);
    assertNotNull("Result is: ", response);
    assertNotNull("Test suite id is: ", response.getTestSuiteId());
    System.out.println("End submit in: " + LoggerUtils.eslapedTime(start));
  }
}
