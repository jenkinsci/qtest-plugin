package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.AutomationTestResultWrapper;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author anpham
 */
public class AutomationTestService {
  private static final String AUTO_TEST_LOG_ENDPOINT_V3_1 = "%s/api/v3.1/projects/%s/test-runs/%s/auto-test-logs?type=jenkins";
  private static final String AUTO_TEST_LOG_ENDPOINT_V3 = "%s/api/v3/projects/%s/test-runs/%s/auto-test-logs/ci/%s";
  private static final String API_SUBMIT_TASK_STATUS = "%s/api/v3/projects/queue-processing/%s";

  public static ResponseEntity push(String buildNumber, String buildPath, List<AutomationTestResult> testResults, JunitSubmitterRequest request, String accessToken)
    throws SubmittedException {

    if (testResults.size() <= 0)
      return null;
    String url;
    AutomationTestResultWrapper wrapper = new AutomationTestResultWrapper();
    wrapper.setBuildNumber(buildNumber);
    wrapper.setBuildPath(buildPath);
    wrapper.setSkipCreatingAutomationModule(true);

    if (request.getSubmitToExistingContainer()) {
      String fullURL = request.getJenkinsServerURL();
      if (!fullURL.endsWith("/")) {
        fullURL += "/";
      }
      fullURL += buildPath;
      for (int i = 0; i < testResults.size(); i++) {
        AutomationTestResult result = testResults.get(i);
        result.setBuildNumber(buildNumber);
        result.setBuildURL(fullURL);
      }
      Long moduleId = request.getModuleID();
      if (null != moduleId &&  0 < moduleId) {
        wrapper.setParent_module(moduleId);
      }
      url = String.format(AUTO_TEST_LOG_ENDPOINT_V3_1, request.getqTestURL(), request.getProjectID(), 0);
      Long testSuiteId = prepareTestSuite(request, accessToken);
      if (-1 == testSuiteId) {
        throw new SubmittedException("Could not find or create test suite to submit test logs", -1);
      }
      wrapper.setTest_suite(testSuiteId);
      wrapper.setTest_logs(testResults);
    } else {
      /**
       * using {@link String#format(Locale, String, Object...)}  instead {@link java.text.MessageFormat#format(String, Object...)}
       * to avoid unexpected formatted link. see: QTE-2798 for more details.
       */
      url = String.format(AUTO_TEST_LOG_ENDPOINT_V3, request.getqTestURL(), request.getProjectID(), 0, request.getConfigurationID());
      wrapper.setTestResults(testResults);
    }

    Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
    ResponseEntity responseEntity = null;
    try {
      String data = JsonUtils.toJson(wrapper);
      responseEntity = HttpClientUtils.post(url, headers, data);
    } catch (ClientRequestException e) {
      throw new SubmittedException(e.getMessage(), null == responseEntity ? 0 : responseEntity.getStatusCode());
    }
    return responseEntity;
  }

  public static ResponseEntity getTaskStatus(String qTestURL, long taskId, Map<String, String> headers)
    throws ClientRequestException {
    String url = String.format(API_SUBMIT_TASK_STATUS, qTestURL, taskId);
    ResponseEntity responseEntity = null;
    try {
      responseEntity = HttpClientUtils.get(url, headers);
    } catch (ClientRequestException e) {
      throw e;
    }

    return responseEntity;
  }

  private static Long createNewTestSuite(JunitSubmitterRequest request, String accessToken, Long parentId, String parentType, String testSuiteName) {
    Object createResult = ConfigService.createTestSuite(request.getqTestURL(),
            accessToken,
            request.getProjectID(),
            parentId,
            parentType,
            testSuiteName,
            request.getEnvironmentParentID(),
            request.getEnvironmentID());
    JSONObject retObj = JSONObject.fromObject(createResult);
    return retObj.optLong("id", -1);
  }

  private static Long findTestSuiteUnderContainerByName(JunitSubmitterRequest request, String accessToken, Long parentId, String parentType, String testSuiteName) {
    Object testSuites = ConfigService.getTestSuiteChildren(request.getqTestURL(), accessToken, request.getProjectID(), parentId, parentType);
    JSONArray suites = JSONArray.fromObject(testSuites);
    Long foundTestSuiteId = -1L;
    if (null != suites && 0 < suites.size()) {
      for (int i = 0; i < suites.size(); i++) {
        String name = suites.getJSONObject(i).getString("name");
        if (0 == testSuiteName.compareTo(name)) {
          foundTestSuiteId = suites.getJSONObject(i).getLong("id");
          break;
        }
      }
    }
    return  foundTestSuiteId;
  }

  private static Long prepareTestSuite(JunitSubmitterRequest request, String accessToken) {

    Date now = new Date();
    SimpleDateFormat ft = new SimpleDateFormat("MM-dd-yyyy");
    String testSuiteName = String.format("%s %s", request.getJenkinsProjectName(), ft.format(now));
    Long nodeId = request.getContainerID();
    String nodeType = request.getContainerType().toLowerCase();
    switch (nodeType) {
      case "release":
      case "test-cycle":
        if (request.getCreateNewTestRunsEveryBuildDate()) {
          testSuiteName = String.format("%s %s", request.getJenkinsProjectName(), ft.format(now));
        } else  {
          testSuiteName = String.format("%s", request.getJenkinsProjectName());
        }
        Long foundTestSuite = findTestSuiteUnderContainerByName(request, accessToken, nodeId, nodeType, testSuiteName);
        if (-1L != foundTestSuite) {
          return foundTestSuite;
        }
        return createNewTestSuite(request, accessToken, nodeId, nodeType, testSuiteName);
      case "test-suite":
        return nodeId;
    }
    return -1L;
  }
}
