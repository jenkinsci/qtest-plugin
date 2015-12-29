/**
 *
 */
package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.AutomationTestResultWrapper;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author anpham
 */
public class AutomationTestService {
  private static final String AUTO_TEST_LOG_ENDPOINT = "%s/api/v3/projects/%s/test-runs/%s/auto-test-logs/ci/%s";
  private static final String API_SUBMIT_TASK_STATUS = "%s/api/v3/projects/queue-processing/%s";

  public static ResponseEntity push(String buildNumber, String buildPath, List<AutomationTestResult> testResults, Configuration configuration, Map<String, String> headers)
    throws SubmittedException {

    if (testResults.size() <= 0)
      return null;

    AutomationTestResultWrapper wrapper = new AutomationTestResultWrapper();

    wrapper.setBuildNumber(buildNumber);
    wrapper.setBuildPath(buildPath);
    wrapper.setTestResults(testResults);

    /**
     * using {@link String#format(Locale, String, Object...)}  instead {@link java.text.MessageFormat#format(String, Object...)}
     * to avoid unexpected formatted link. see: QTE-2798 for more details.
     */
    String url = String.format(AUTO_TEST_LOG_ENDPOINT, configuration.getUrl(), configuration.getProjectId(), 0, configuration.getId());

    ResponseEntity responseEntity = null;
    try {
      responseEntity = HttpClientUtils.post(url, headers, JsonUtils.toJson(wrapper));
    } catch (ClientRequestException e) {
      throw new SubmittedException(e.getMessage(), null == responseEntity ? 0 : responseEntity.getStatusCode());
    }
    return responseEntity;
  }

  /**
   * @param configuration
   * @param taskId
   * @param headers
   * @return
   * @throws ClientRequestException
   */
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
}
