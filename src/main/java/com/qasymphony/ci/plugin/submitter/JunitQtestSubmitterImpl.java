package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResponse;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.model.AbstractBuild;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class JunitQtestSubmitterImpl implements JunitSubmitter {
  private static final Logger LOG = Logger.getLogger(JunitQtestSubmitterImpl.class.getName());
  private StoreResultService storeResultService = new StoreResultServiceImpl();

  @Override public JunitSubmitterResult submit(JunitSubmitterRequest request) throws SubmittedException {
    try {
      AutomationTestResponse response = AutomationTestService.push(request.getBuildNumber(), request.getBuildPath(), request.getTestResults(), request.getConfiguration(),
        OauthProvider.buildHeaders(request.getConfiguration().getUrl(), request.getConfiguration().getAppSecretKey(), null));
      JunitSubmitterResult result = new JunitSubmitterResult()
        .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
        .setTestSuiteId(null)
        .setNumberOfTestLog(request.getTestResults().size())
        .setNumberOfTestResult(request.getTestResults().size())
        .setTestSuiteName("");
      if (response == null)
        return result;

      result.setTestSuiteId(response.getTestSuiteId())
        .setNumberOfTestLog(response.getTotalTestLogs())
        .setSubmittedStatus(JunitSubmitterResult.STATUS_SUCCESS)
        .setTestSuiteName(response.getTestSuiteName())
        .setNumberOfTestResult(response.getTotalTestCases());
      return result;
    } catch (Exception se) {
      LOG.log(Level.WARNING, se.getMessage(), se);
      throw new SubmittedException(se);
    }
  }

  @Override public SubmittedResult storeSubmittedResult(AbstractBuild build, JunitSubmitterResult result)
    throws StoreResultException {
    SubmittedResult submitResult = new SubmittedResult()
      .setBuildNumber(build.getNumber())
      .setStatusBuild(build.getResult().toString())
      .setTestSuiteId(result.getTestSuiteId())
      .setTestSuiteName(result.getTestSuiteName())
      .setSubmitStatus(result.getSubmittedStatus())
      .setNumberTestLog(result.getNumberOfTestLog())
      .setNumberTestResult(result.getNumberOfTestResult());
    try {
      storeResultService.store(build.getProject(), submitResult);
      return submitResult;
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage(), e);
      throw new StoreResultException("Cannot store result." + e.getMessage(), e);
    }
  }
}
