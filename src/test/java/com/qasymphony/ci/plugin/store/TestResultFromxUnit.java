package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.parse.CommonParsingUtils;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.parse.ParseRequest;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
public class TestResultFromxUnit extends TestAbstracts {

  public static final class TestResultFromxUnitProject extends Builder implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Configuration configuration;
    private List<AutomationTestResult> automationTestResultList;

    public TestResultFromxUnitProject(Configuration configuration) {
      this.configuration = configuration;
    }

    public List<AutomationTestResult> getAutomationTestResultList() {
      return automationTestResultList;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build,
      Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
      try {
        File currentBasedDir = new File(build.getWorkspace().toURI());
        List<String> matchDirs = CommonParsingUtils.scanJunitTestResultFolder(currentBasedDir.getPath());
        long current = System.currentTimeMillis();
        for (String dir : matchDirs) {
          File testFolder = new File(currentBasedDir.getPath(), dir);
          testFolder.setLastModified(current);
          for (File file : testFolder.listFiles()) {
            file.setLastModified(current);
          }
        }
        automationTestResultList = JunitTestResultParser.parse(new ParseRequest()
          .setBuild(build)
          .setListener(listener)
          .setLauncher(launcher)
          .setConfiguration(configuration));
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }
  }

  private FreeStyleProject project;

  @Before public void setUp() throws Exception {
  }

  @LocalData
  @Test public void testParser()
    throws InterruptedException, ExecutionException, TimeoutException, IOException {
    project = j.createFreeStyleProject("xunit-project");

    Configuration configuration = Configuration.newInstance();
    configuration.setReadFromJenkins(true);
    configuration.setProjectName("TestResultFromxUnitProject");
//    configuration.setAppSecretKey("34428172-cdba-4c97-aefb-b2ee5ae5db99");
    configuration.setAppSecretKey("5d65f50e-b368-47a1-9bff-e0a710011a3f");
    configuration.setReleaseId(1L);
    configuration.setId(1L);
    configuration.setProjectId(1L);
    configuration.setUrl("https://localhost:7443");
    configuration.setJenkinsProjectName("TestResultFromxUnitProject");
    configuration.setJenkinsServerUrl("http://localhost:8080/jenkins");

    TestResultFromxUnitProject testResultFromxUnitProject = new TestResultFromxUnitProject(configuration);
    configuration.setResultPattern("*.xml");
    project.getBuildersList().add(testResultFromxUnitProject);
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("", testResultFromxUnitProject.getAutomationTestResultList());
    assertNotNull("Build is: ", build);
    String buildNumber = "1";
    String buildPath = "/jobs/TestResultFromxUnitProject/" + buildNumber;
    Map<String, String> headers = OauthProvider.buildHeaders(configuration.getUrl(), configuration.getAppSecretKey(), null);
    try {
      AutomationTestService.push(buildNumber, buildPath, testResultFromxUnitProject.getAutomationTestResultList(), configuration, headers);
    } catch (SubmittedException e) {
      e.printStackTrace();
    }
  }

  @LocalData
  @Test public void testSubmit()
    throws InterruptedException, ExecutionException, TimeoutException, IOException, SubmittedException {
    project = j.createFreeStyleProject("xunit-project");
    String buildNumber = "1";
    String buildPath = "/jobs/TestResultFromxUnitProject/" + buildNumber;
    String projectName = "TestResultFromxUnitProject";
    String apiKey = "5d65f50e-b368-47a1-9bff-e0a710011a3f";
    Long releaseId = 1L;
    Long ciId = 1L;
    Long qTestProjectId = 1L;
    Configuration configuration = new Configuration(ciId, "https://localhost:7443", apiKey, qTestProjectId, projectName,
      releaseId, "releaseName", 0L, "environment", 0L, 0L, false, "");

    configuration.setResultPattern("*.xml");

    TestResultFromxUnitProject testResultFromxUnitProject = new TestResultFromxUnitProject(configuration);
    project.getBuildersList().add(testResultFromxUnitProject);
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);

    Map<String, String> headers = OauthProvider.buildHeaders(configuration.getUrl(), configuration.getAppSecretKey(), null);
    AutomationTestService.push(buildNumber, buildPath, testResultFromxUnitProject.getAutomationTestResultList(), configuration, headers);
  }

}
