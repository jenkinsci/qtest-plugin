package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResponse;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.model.AbstractBuild;
import org.apache.commons.lang.StringUtils;

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
    String accessToken = OauthProvider.getAccessToken(request.getConfiguration().getUrl(), request.getConfiguration().getAppSecretKey());
    if (StringUtils.isEmpty(accessToken))
      throw new SubmittedException(String.format("Cannot get access token from: %s, access token is: %s",
        request.getConfiguration().getUrl(), request.getConfiguration().getAppSecretKey()));

    AutomationTestResponse response = AutomationTestService.push(request.getBuildNumber(), request.getBuildPath(),
      request.getTestResults(), request.getConfiguration(), OauthProvider.buildHeaders(accessToken, null));
    //TODO: implement with new task status api in qTest code
    JunitSubmitterResult result = new JunitSubmitterResult()
      .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
      .setTestSuiteId(null)
      .setNumberOfTestLog(0)
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
  }

  @Override public SubmittedResult storeSubmittedResult(AbstractBuild build, JunitSubmitterResult result)
    throws StoreResultException {
    //get saved configuration
    Configuration configuration = ConfigService.getPluginConfiguration(build.getProject());
    String qTestUrl = configuration == null ? "" : configuration.getUrl();
    Long projectId = configuration == null ? 0L : configuration.getProjectId();

    SubmittedResult submitResult = new SubmittedResult()
      .setUrl(qTestUrl)
      .setProjectId(projectId)
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
