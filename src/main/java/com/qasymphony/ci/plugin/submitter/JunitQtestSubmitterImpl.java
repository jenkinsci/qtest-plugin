package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.Constants;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResponse;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.model.qtest.SubmittedTask;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import hudson.model.AbstractBuild;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

  @Override public JunitSubmitterResult submit(JunitSubmitterRequest request) throws Exception {
    String accessToken = OauthProvider.getAccessToken(request.getConfiguration().getUrl(), request.getConfiguration().getAppSecretKey());
    if (StringUtils.isEmpty(accessToken))
      throw new SubmittedException(String.format("Cannot get access token from: %s, access token is: %s",
        request.getConfiguration().getUrl(), request.getConfiguration().getAppSecretKey()));

    ResponseEntity responseEntity = AutomationTestService.push(request.getBuildNumber(), request.getBuildPath(),
      request.getTestResults(), request.getConfiguration(), OauthProvider.buildHeaders(accessToken, null));
    AutomationTestResponse response = null;
    if (responseEntity.getStatusCode() == HttpStatus.SC_CREATED) {
      //receive task response
      SubmittedTask task = JsonUtils.fromJson(responseEntity.getBody(), SubmittedTask.class);
      if (task == null || task.getId() <= 0)
        throw new SubmittedException(responseEntity.getBody(), responseEntity.getStatusCode());
      response = getSubmitLogResponse(request, task);
    } else {
      //if cannot passed validation from qTest
      throw new SubmittedException(ConfigService.getErrorMessage(responseEntity.getBody()), responseEntity.getStatusCode());
    }

    Boolean nullResponse = (null == response);
    Boolean isSubmitSuccess = ((!nullResponse && response.getTestSuiteId() > 0) ? true : false);
    JunitSubmitterResult result = new JunitSubmitterResult()
      .setNumberOfTestResult(request.getTestResults().size())
      .setTestSuiteId(nullResponse ? null : response.getTestSuiteId())
      .setTestSuiteName(nullResponse ? "" : response.getTestSuiteName())
      .setNumberOfTestLog(nullResponse ? 0 : response.getTotalTestLogs())
      .setSubmittedStatus(isSubmitSuccess ? JunitSubmitterResult.STATUS_SUCCESS : JunitSubmitterResult.STATUS_FAILED);
    return result;
  }

  private AutomationTestResponse getSubmitLogResponse(JunitSubmitterRequest request, SubmittedTask task)
    throws InterruptedException, SubmittedException {
    if (task == null || task.getId() <= 0)
      return null;

    AutomationTestResponse response = null;
    PrintStream logger = request.getListener().getLogger();
    Map<String, String> headers = OauthProvider.buildHeaders(request.getConfiguration().getUrl(), request.getConfiguration().getAppSecretKey(), null);
    Boolean mustRetry = true;
    String previousState = "";
    while (mustRetry) {
      response = getTaskResponse(request, task, headers);
      if (null == response) {
        mustRetry = false;
      } else {
        if (!previousState.equalsIgnoreCase(response.getState())) {
          LoggerUtils.formatInfo(logger, "%s: Submission status: %s", JsonUtils.getCurrentDateString(), response.getState());
          previousState = StringUtils.isEmpty(response.getState()) ? "" : response.getState();
        }
        if (response.hasError()) {
          //if has error while get task status
          LoggerUtils.formatError(logger, "   %s", ConfigService.getErrorMessage(response.getContent()));
        }
        if (Constants.LIST_FINISHED_STATE.contains(response.getState())) {
          //if finished, we do not retry more
          mustRetry = false;
        } else {
          //sleep in interval to get status of task
          Thread.sleep(Constants.RETRY_INTERVAL);
        }
      }
    }
    return response;
  }

  private AutomationTestResponse getTaskResponse(JunitSubmitterRequest request, SubmittedTask task, Map<String, String> headers)
    throws SubmittedException {
    ResponseEntity responseEntity;
    try {
      //get task status
      responseEntity = AutomationTestService.getTaskStatus(request.getConfiguration(), task.getId(), headers);
    } catch (ClientRequestException e) {
      LoggerUtils.formatError(request.getListener().getLogger(), "Cannot get response of taskId: %s, error: %s", task.getId(), e.getMessage());
      throw new SubmittedException(e.getMessage(), -1);
    }
    LOG.info(String.format("project:%s, status:%s, body:%s", request.getConfiguration().getJenkinsProjectName(),
      null == responseEntity ? -1 : responseEntity.getStatusCode(), null == responseEntity ? "" : responseEntity.getBody()));

    if ((null == responseEntity) || (responseEntity.getStatusCode() != HttpStatus.SC_OK)) {
      throw new SubmittedException(ConfigService.getErrorMessage(responseEntity.getBody()), responseEntity.getStatusCode());
    }
    return new AutomationTestResponse(responseEntity.getBody());
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
