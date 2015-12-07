package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.recipes.LocalData;

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
      .setNumberTestLog(0)
      .setBuildNumber(currentBuild)
      .setStatusBuild("SUCCESS");
    storeResultService.store(project, result);
    Map<Integer, SubmittedResult> resMap = storeResultService.fetchAll(new ReadSubmitLogRequest()
      .setProject(project)).getResults();

    assertEquals("Result size is: ", 1, resMap.size());
    SubmittedResult submittedResult = resMap.get(1);
    assertNotNull("submittedResult is ", submittedResult);
    assertEquals("Build number is: ", result.getBuildNumber(), submittedResult.getBuildNumber());
    assertEquals("Status build is: ", result.getStatusBuild(), submittedResult.getStatusBuild());
    assertEquals("TestSuite name is: ", result.getTestSuiteName(), submittedResult.getTestSuiteName());
    assertEquals("Submit status is: ", result.getSubmitStatus(), submittedResult.getSubmitStatus());
    assertEquals("Number of testCase is: ", result.getNumberTestResult(), submittedResult.getNumberTestResult());
    assertEquals("Number of testLog is: ", result.getNumberTestLog(), submittedResult.getNumberTestLog());
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
      .setNumberTestLog(0)
      .setBuildNumber(currentBuild)
      .setStatusBuild("SUCCESS");
    storeResultService.store(project, result);
    SubmittedResult result2 = new SubmittedResult()
      .setSubmitStatus(JunitSubmitterResult.STATUS_SUCCESS)
      .setTestSuiteName(project.getName())
      .setNumberTestResult(2)
      .setNumberTestLog(2)
      .setBuildNumber(++currentBuild)
      .setStatusBuild("FAILED");
    storeResultService.store(project, result2);
    Map<Integer, SubmittedResult> resMap = storeResultService.fetchAll(new ReadSubmitLogRequest()
      .setProject(project)).getResults();

    assertEquals("Result size is: ", 2, resMap.size());
    assertNotNull("Result 0 is: ", resMap.get(1));
    assertNotNull("Result 1 is: ", resMap.get(2));
  }
}
