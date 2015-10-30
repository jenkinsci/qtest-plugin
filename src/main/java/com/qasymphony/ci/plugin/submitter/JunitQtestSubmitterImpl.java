package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.AutomationTestResponse;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.model.AbstractBuild;

import java.io.IOException;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class JunitQtestSubmitterImpl implements JunitSubmitter {
  private StoreResultService storeResultService = new StoreResultServiceImpl();

  @Override public JunitSubmitterResult submit(JunitSubmitterRequest request) throws Exception {
    AutomationTestResponse response = AutomationTestService.push(request.getTestResults(), request.getConfiguration(),
      OauthProvider.buildHeader(request.getConfiguration().getAppSecretKey(), null));
    JunitSubmitterResult result = new JunitSubmitterResult()
      .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
      .setTestSuiteId(null)
      .setNumberOfTestRun(request.getTestResults().size())
      .setNumberOfTestResult(request.getTestResults().size())
      .setTestSuiteName("");
    if (response == null)
      return result;

    result.setTestSuiteId(response.getTestSuiteId())
      .setNumberOfTestRun(response.getTotalTestRuns())
      .setSubmittedStatus(JunitSubmitterResult.STATUS_SUCCESS)
      .setTestSuiteName(response.getTestSuiteName())
      .setNumberOfTestResult(response.getTotalTestCases());
    return result;
  }

  @Override public SubmittedResult storeSubmittedResult(AbstractBuild build, JunitSubmitterResult result)
    throws IOException, InterruptedException, StoreResultException {
    SubmittedResult submitResult = new SubmittedResult()
      .setBuildNumber(build.getNumber())
      .setStatusBuild(build.getResult().toString())
      .setTestSuiteName(result.getTestSuiteName())
      .setSubmitStatus(result.getSubmittedStatus())
      .setNumberTestRun(result.getNumberOfTestRun())
      .setNumberTestResult(result.getNumberOfTestResult());
    storeResultService.store(build.getProject(), submitResult);
    return submitResult;
  }
}
