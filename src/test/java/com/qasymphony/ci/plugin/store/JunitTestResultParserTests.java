package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.*;
import com.qasymphony.ci.plugin.parse.CommonParsingUtils;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.parse.ParseRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.testsupport.LocalHttpsFixture;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.tasks.junit.CaseResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
        File currentBasedDir = new File(Objects.requireNonNull(build.getWorkspace()).toURI());
        List<String> matchDirs = CommonParsingUtils.scanJunitTestResultFolder(currentBasedDir.getPath());
        long current = System.currentTimeMillis();
        for (String dir : matchDirs) {
          File testFolder = new File(currentBasedDir.getPath(), dir);
          testFolder.setLastModified(current);
          for (File file : Objects.requireNonNull(testFolder.listFiles())) {
            file.setLastModified(current);
          }
        }
        automationTestResultList = JunitTestResultParser.parse(new ParseRequest()
          .setBuild(build)
          .setWorkSpace(build.getWorkspace())
          .setListener(listener)
          .setLauncher(launcher)
          .setUtilizeTestResultFromCITool(true)
          .setCreateEachMethodAsTestCase(false)
          .setOverwriteExistingTestSteps(false)
          );
      } catch (Exception e) {
        e.printStackTrace();
        throw new IOException("JUnitParserTestAntProject.perform failed", e);
      }
      return true;
    }
  }

  // Not real secrets: fixed, non-random placeholder values for a fake qTest project used only
  // to exercise AutomationTestService.push() against the local fixture server below.
  private static final String FAKE_ANT_PROJECT_KEY = "fake-ant-project-api-key";
  private static final String FAKE_PERFORMANCE_PROJECT_KEY = "fake-performance-project-api-key";

  private FreeStyleProject project;
  private static List<AutomationTestResult> automationTestResultList;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  private LocalHttpsFixture qtestFixture;

  @Before public void setUp() throws Exception {
    // The submission-path tests below used to point at a dead https://localhost:7443 with no
    // server behind it. Stand up a real local HTTPS server (self-signed cert) as a stand-in
    // qTest endpoint, and use HttpClientUtils' documented escape hatch for on-prem self-signed
    // servers (see QTEST-39511) rather than importing a throwaway cert into a trust store.
    HttpClientUtils.resetClient();
    System.setProperty(HttpClientUtils.ALLOW_INSECURE_SSL_PROPERTY, "true");
    qtestFixture = LocalHttpsFixture.startRespondingWith(tempFolder.getRoot(), "{}");
  }

  @After public void tearDown() {
    if (qtestFixture != null) {
      qtestFixture.close();
    }
    HttpClientUtils.resetClient();
    System.clearProperty(HttpClientUtils.ALLOW_INSECURE_SSL_PROPERTY);
  }

  @LocalData
  @Test public void testAntResultProject()
    throws InterruptedException, ExecutionException, TimeoutException, IOException {

    project = j.createFreeStyleProject("ant-project");
    automationTestResultList = null;
    project.getBuildersList().add(new JUnitParserTestAntProject());
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);
    // The ant-project fixture's build/test/ also contains TEST-method-with-params.xml, a
    // misplaced/mislabeled fixture holding 226 NUnit.ProjectEditor.Tests.* cases across 22
    // distinct classes inside a <testsuites> (plural) root -- a different format from the
    // real HelloWorldTest's bare <testsuite> root. The historical "1" expectation here relied
    // on an old JUnitParser silently failing to parse the <testsuites> wrapper (swallowed by
    // AutoScanParser's per-folder catch), which no longer happens on a modern Jenkins core.
    // 22 NUnit classes + 1 real helloWorld.HelloWorldTest = 23.
    assertEquals("", 23, automationTestResultList.size());
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
    String apiKey = FAKE_ANT_PROJECT_KEY;
    String secretKey = FAKE_ANT_PROJECT_KEY;
    long releaseId = 1L;
    Long ciId = 1L;
    long qTestProjectId = 3L;
    Configuration configuration = new Configuration(ciId, qtestFixture.getBaseUrl(), apiKey, secretKey, qTestProjectId, projectName,
      releaseId, "releaseName", 0L, "environment", 0L, 0L, false, "", false, "{}" ,
            false,
            0);
    JunitSubmitterRequest submitterRequest = configuration.createJunitSubmitRequest();
    submitterRequest.setBuildNumber("1")
            .setBuildPath(buildPath);

    ResponseEntity response = AutomationTestService.push(buildNumber, buildPath, automationTestResultList, submitterRequest, configuration.getAppSecretKey());
    assertNotNull("push() should have reached the fixture server and returned a response", response);
    assertEquals(Integer.valueOf(200), response.getStatusCode());
  }

  @Test public void testSubmitLog() throws SubmittedException {
    String buildNumber = "1";
    String buildPath = "/jobs/TestPerformance/" + buildNumber;
    String projectName = "TestPerformance";
    String apiKey = FAKE_PERFORMANCE_PROJECT_KEY;
    String secretKey = FAKE_PERFORMANCE_PROJECT_KEY;
    long releaseId = 1L;
    Long ciId = 3L;
    long qTestProjectId = 1L;
    Configuration configuration = new Configuration(
            ciId,
            qtestFixture.getBaseUrl(),
            apiKey,
            secretKey,
            qTestProjectId,
            projectName,
            releaseId,
            "releaseName",
            0L,
            "environment",
            0L,
            0L,
            false,
            "",
            false,
            "{}" ,
            false,
            0);
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
      List<AutomationTestStepLog> testLogs = new ArrayList<>();
      for (int j = 0; j < 10; j++) {
        AutomationTestStepLog automationTestStepLog = new AutomationTestStepLog();
        automationTestStepLog.setOrder(j);
        automationTestStepLog.setStatus(CaseResult.Status.PASSED.toString());
        automationTestStepLog.setDescription("Test Description of " + j + " in class: " + i);
        automationTestStepLog.setExpectedResult(CaseResult.Status.PASSED.toString());
        testLogs.add(automationTestStepLog);
      }
      automationTestResult.setTestLogs(testLogs);
    }
    JunitSubmitterRequest submitterRequest = configuration.createJunitSubmitRequest();
    ResponseEntity response = AutomationTestService.push(buildNumber, buildPath, results, submitterRequest, configuration.getAppSecretKey());
    assertNotNull("push() should have reached the fixture server and returned a response", response);
    assertEquals(Integer.valueOf(200), response.getStatusCode());
  }

  @Test public void testSubmitLogWithAttachment() throws SubmittedException {
    String buildNumber = "1";
    String buildPath = "/jobs/TestPerformance/" + buildNumber;
    String projectName = "TestPerformance";
    String apiKey = FAKE_PERFORMANCE_PROJECT_KEY;
    String secretKey = FAKE_PERFORMANCE_PROJECT_KEY;
    long releaseId = 1L;
    Long ciId = 3L;
    long qTestProjectId = 1L;
    Configuration configuration = new Configuration(ciId, qtestFixture.getBaseUrl(), apiKey, secretKey, qTestProjectId, projectName,
      releaseId, "releaseName", 0L, "environment", 0L, 0L, false, "", false,"{}" ,
            false,
            0);
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
      List<AutomationTestStepLog> testLogs = new ArrayList<>();
      List<AutomationAttachment> automationAttachments = new ArrayList<>();
      for (int j = 0; j < 100; j++) {
        AutomationTestStepLog automationTestStepLog = new AutomationTestStepLog();
        automationTestStepLog.setOrder(j);
        automationTestStepLog.setStatus(CaseResult.Status.FAILED.toString());
        automationTestStepLog.setDescription("Test Description of " + j + " in class: " + i);
        automationTestStepLog.setExpectedResult(CaseResult.Status.FAILED.toString());
        testLogs.add(automationTestStepLog);

        AutomationAttachment automationAttachment = new AutomationAttachment();
        automationAttachment.setName(automationTestStepLog.getDescription() + ".txt");
        automationAttachment.setContentType("text/plain");
        automationAttachment.setData("Test attachment data".repeat(10));
        automationAttachments.add(automationAttachment);
      }
      automationTestResult.setTestLogs(testLogs);
      automationTestResult.setAttachments(automationAttachments);
    }
    JunitSubmitterRequest submitterRequest = configuration.createJunitSubmitRequest();
    ResponseEntity response = AutomationTestService.push(buildNumber, buildPath, results, submitterRequest, configuration.getAppSecretKey());
    assertNotNull("push() should have reached the fixture server and returned a response", response);
    assertEquals(Integer.valueOf(200), response.getStatusCode());
    System.out.println("End submit in: " + LoggerUtils.elapsedTime(start));
  }
}
