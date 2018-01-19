package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.AutomationTestResultWrapper;
import com.qasymphony.ci.plugin.model.Configuration;
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
  private static final String AUTO_TEST_LOG_ENDPOINT = "%s/api/v3.1/projects/%s/test-runs/%s/auto-test-logs?type=automation";
  private static final String API_SUBMIT_TASK_STATUS = "%s/api/v3/projects/queue-processing/%s";

  public static ResponseEntity push(String buildNumber, String buildPath, List<AutomationTestResult> testResults, Configuration configuration, String accessToken)
    throws SubmittedException {

    if (testResults.size() <= 0)
      return null;
    String fullURL = configuration.getJenkinsServerUrl();
    if (!fullURL.endsWith("/")) {
      fullURL += "/";
    }
    fullURL += buildPath;
    for (int i = 0; i < testResults.size(); i++) {
      AutomationTestResult result = testResults.get(i);
      result.setBuildNumber(buildNumber);
      result.setBuildURL(fullURL);
    }
    AutomationTestResultWrapper wrapper = new AutomationTestResultWrapper();

    wrapper.setBuildNumber(buildNumber);
    Long moduleId = configuration.getModuleId();
    if (0 < moduleId) {
      wrapper.setParent_module(moduleId);
    }
    wrapper.setBuildPath(buildPath);
    wrapper.setTest_logs(testResults);

    Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
    /**
     * using {@link String#format(Locale, String, Object...)}  instead {@link java.text.MessageFormat#format(String, Object...)}
     * to avoid unexpected formatted link. see: QTE-2798 for more details.
     */
    String url = String.format(AUTO_TEST_LOG_ENDPOINT, configuration.getUrl(), configuration.getProjectId(), 0);

    ResponseEntity responseEntity = null;
    try {
      Long testSuiteId = prepareTestSuite(configuration, accessToken);
      if (-1 == testSuiteId) {
        throw new SubmittedException("Could not find or create test suite to submit test logs");
      }
      wrapper.setTest_suite(testSuiteId);

      String data = JsonUtils.toJson(wrapper);
      responseEntity = HttpClientUtils.post(url, headers, data);
    } catch (ClientRequestException e) {
      throw new SubmittedException(e.getMessage(), null == responseEntity ? 0 : responseEntity.getStatusCode());
    }
    return responseEntity;
  }

  public static ResponseEntity getTaskStatus(Configuration configuration, long taskId, Map<String, String> headers)
    throws ClientRequestException {
    String url = String.format(API_SUBMIT_TASK_STATUS, configuration.getUrl(), taskId);
    ResponseEntity responseEntity = null;
    try {
      responseEntity = HttpClientUtils.get(url, headers);
    } catch (ClientRequestException e) {
      throw e;
    }

    return responseEntity;
  }

  private static Long createNewTestSuite(Configuration configuration, String accessToken, Long parentId, String parentType, String testSuiteName) {
    Object createResult = ConfigService.createTestSuite(configuration.getUrl(), accessToken, configuration.getProjectId(), parentId, parentType, testSuiteName);
    JSONObject retObj = JSONObject.fromObject(createResult);
    return retObj.getLong("id");
  }

  private static Long findTestSuiteUnderContainerByName(Configuration configuration, String accessToken, Long parentId, String parentType, String testSuiteName) {
    Object testSuites = ConfigService.getTestSuiteChildren(configuration.getUrl(), accessToken, configuration.getProjectId(), parentId, parentType);
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

  private static Long prepareTestSuite(Configuration configuration, String accessToken) {
//    Long prevTestSuiteId = configuration.getTestSuiteId();
//    if (0 < prevTestSuiteId) {
//      return prevTestSuiteId;
//    }

    Date now = new Date();
    SimpleDateFormat ft = new SimpleDateFormat("MM-dd-yyyy");
    String testSuiteName = String.format("%s %s", configuration.getJenkinsProjectName(), ft.format(now));
    if (!configuration.isSubmitToContainer()) {
      // submit to release
      // create/get testsuite under create configuration.getReleaseId()
      Long foundTestSuiteId = findTestSuiteUnderContainerByName(configuration, accessToken, configuration.getReleaseId(), "release", testSuiteName);
      if (-1L != foundTestSuiteId) {
        return foundTestSuiteId;
      }
      // create new test suite
      return createNewTestSuite(configuration, accessToken, configuration.getReleaseId(), "release", testSuiteName );
    } else {
      JSONObject json = configuration.getContainerJSONObject();
      if (null == json) {
        return -1L;
      }
      JSONArray containerPath = json.getJSONArray("containerPath");
      if (null == containerPath) {
        return -1L;
      }
      int pathSize = containerPath.size();
      if (0 < pathSize) {
        JSONObject jsonNode = containerPath.getJSONObject(pathSize - 1);
        Long nodeId = jsonNode.getLong("nodeId");
        String nodeType = jsonNode.getString("nodeType");
        switch (nodeType) {
          case "release":
          case "test-cycle":
            if (configuration.isCreateNewTestSuiteEveryBuild()) {
              testSuiteName = String.format("%s %s", configuration.getJenkinsProjectName(), ft.format(now));
            } else  {
              testSuiteName = String.format("%s", configuration.getJenkinsProjectName());
            }
            Long foundTestSuite = findTestSuiteUnderContainerByName(configuration, accessToken, nodeId, nodeType, testSuiteName);
            if (-1L != foundTestSuite) {
              return foundTestSuite;
            }
            return createNewTestSuite(configuration, accessToken, nodeId, nodeType, testSuiteName);
          case "test-suite":
            return nodeId;
        }
      }
    }
    return -1L;
  }
}
