package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import net.sf.json.JSONObject;

import java.io.IOException;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class JunitQtestSubmitterImpl implements JunitSubmitter {
  private StoreResultService storeResultService = new StoreResultServiceImpl();

  @Override public JunitSubmitterResult submit(JunitSubmitterRequest request) throws Exception {
    ResponseEntity entity = AutomationTestService.push(request.getTestResults(), request.getConfiguration(),
      OauthProvider.buildHeader(request.getConfiguration().getAppSecretKey(), null));
    JunitSubmitterResult result = new JunitSubmitterResult()
      .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
      .setTestSuiteId(null)
      .setNumberOfTestRun(request.getTestResults().size())
      .setNumberOfTestResult(request.getTestResults().size())
      .setTestSuiteName("");
    if (null == entity)
      return result;
    JSONObject jsonObject = JSONObject.fromObject(entity.getBody());
    Long testSuiteId = jsonObject.getLong("test_suite_id");
    String testSuiteName = jsonObject.getString("test_suite_name");
    Integer testRuns = jsonObject.getInt("test_runs");

    result.setTestSuiteId(testSuiteId)
      .setNumberOfTestRun(null == testRuns ? 0 : testRuns)
      .setSubmittedStatus(JunitSubmitterResult.STATUS_SUCCESS)
      .setTestSuiteName(testSuiteName == null ? "" : testSuiteName);
    return result;
  }

  @Override public SubmittedResult storeSubmittedResult(AbstractBuild build, JunitSubmitterResult result)
    throws IOException, InterruptedException {
    final FilePath filePath = build.getWorkspace();
    SubmittedResult submitResult = new SubmittedResult()
      .setBuildNumber(build.getNumber())
      .setStatusBuild(build.getResult().toString())
      .setTestSuiteName(result.getTestSuiteName())
      .setSubmitStatus(result.getSubmittedStatus())
      .setNumberTestRun(result.getNumberOfTestRun())
      .setNumberTestResult(result.getNumberOfTestResult());
    storeResultService.store(filePath, submitResult);
    return submitResult;
  }
}
