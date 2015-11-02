package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.Functions;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StoreResultServiceTests extends TestAbstracts {
  private FreeStyleProject project;
  private StoreResultServiceImpl storeResultService;

  @Before public void setUp() throws Exception {
    project = j.createFreeStyleProject("jqtest-test-store-result");
    storeResultService = new StoreResultServiceImpl();
  }

  @LocalData
  @Test public void testStore()
    throws Exception {
    project.scheduleBuild2(0)
      .get(1, TimeUnit.MINUTES);
    int currentBuild = project.getNextBuildNumber() - 1;
    SubmittedResult result = new SubmittedResult()
      .setSubmitStatus(JunitSubmitterResult.STATUS_SUCCESS)
      .setTestSuiteName(project.getName())
      .setNumberTestResult(0)
      .setNumberTestResult(0)
      .setBuildNumber(currentBuild)
      .setStatusBuild("SUCCESS");
    storeResultService.store(project, result);
    Map<Integer, SubmittedResult> resMap = storeResultService.fetchAll(project, currentBuild);

    assertEquals("Result size is: ", 1, resMap.size());
    SubmittedResult submittedResult = resMap.get(0);
    assertNotNull("submittedResult is ", submittedResult);
    assertEquals("Result is: ", JsonUtils.toJson(result), JsonUtils.toJson(submittedResult));
  }

  @LocalData
  @Test public void testStore2()
    throws Exception {
    project.scheduleBuild2(0)
      .get(1, TimeUnit.MINUTES);
    int currentBuild = project.getNextBuildNumber() - 1;
    SubmittedResult result = new SubmittedResult()
      .setSubmitStatus(JunitSubmitterResult.STATUS_SUCCESS)
      .setTestSuiteName(project.getName())
      .setNumberTestResult(0)
      .setNumberTestRun(0)
      .setBuildNumber(currentBuild)
      .setStatusBuild("SUCCESS");
    storeResultService.store(project, result);
    SubmittedResult result2 = new SubmittedResult()
      .setSubmitStatus(JunitSubmitterResult.STATUS_SUCCESS)
      .setTestSuiteName(project.getName())
      .setNumberTestResult(2)
      .setNumberTestRun(2)
      .setBuildNumber(++currentBuild)
      .setStatusBuild("FAILED");
    storeResultService.store(project, result2);
    Map<Integer, SubmittedResult> resMap = storeResultService.fetchAll(project, currentBuild);

    assertEquals("Result size is: ", 2, resMap.size());
    assertEquals("Result 0 is: ", JsonUtils.toJson(result), JsonUtils.toJson(resMap.get(result.getBuildNumber())));
    assertEquals("Result 1 is: ", JsonUtils.toJson(result2), JsonUtils.toJson(resMap.get(result2.getBuildNumber())));
  }
}
