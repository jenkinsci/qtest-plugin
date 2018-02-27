package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.AutomationTestStepLog;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.parse.CommonParsingUtils;
import com.qasymphony.ci.plugin.parse.PatternScanParser;
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
public class MavenJunitParseTests extends TestAbstracts {

  public static final class MavenParseTestMavenProject extends Builder implements Serializable {
    private static final long serialVersionUID = 1L;

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
        automationTestResultList = new PatternScanParser().parse(new ParseRequest()
        .setLauncher(launcher)
        .setListener(listener)
        .setBuild(build)
        .setUtilizeTestResultFromCITool(true));
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
  @Test public void testParseMavenProject()
    throws InterruptedException, ExecutionException, TimeoutException, IOException {

    project = j.createFreeStyleProject("maven-project");
    automationTestResultList = null;
    project.getBuildersList().add(new MavenParseTestMavenProject());
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);
    assertEquals("", 8, automationTestResultList.size());
    AutomationTestResult calculateTest = null;
    for (AutomationTestResult automationTestResult : automationTestResultList) {
      if (automationTestResult.getName().equalsIgnoreCase("sample.junit.CalculateTest")) {
        calculateTest = automationTestResult;
        break;
      }
    }
    assertNotNull("Calculate test is:", calculateTest);
    assertEquals("Test log size is ", 4, calculateTest.getTestLogs().size());
    AutomationTestStepLog first = calculateTest.getTestLogs().get(0);
    assertEquals("Description 1 is ", "testSum_second", first.getDescription());
    assertEquals("Status 1 is ", "FAILED", first.getStatus());

    AutomationTestStepLog second = calculateTest.getTestLogs().get(1);
    assertEquals("Description 2 is ", "testSum_one", second.getDescription());
    assertEquals("Status 2 is ", "PASSED", second.getStatus());
  }
}
