/**
 *
 */
package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.*;
import com.qasymphony.ci.plugin.model.Error;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * @author anpham
 */
public class AutomationTestService {
  private static String AUTO_TEST_LOG_ENDPOINT = "api/v3/projects/{0}/test-runs/{1}/auto-test-logs/ci/{2}";

  public static AutomationTestResponse push(String buildNumber, String buildPath, List<AutomationTestResult> testResults, Configuration configuration, Map<String, String> headers)
    throws Exception {

    if (testResults.size() <= 0)
      return null;

    AutomationTestResultWrapper wrapper = new AutomationTestResultWrapper();

    wrapper.setBuildNumber(buildNumber);
    wrapper.setBuildPath(buildPath);
    wrapper.setTestResults(testResults);

    String url = String.format("%s/%s", configuration.getUrl(),
      MessageFormat.format(AUTO_TEST_LOG_ENDPOINT, new Object[] {configuration.getProjectId(), 0, configuration.getId()}));

    ResponseEntity responseEntity = HttpClientUtils.post(url, headers, JsonUtils.toJson(wrapper));

    if (responseEntity.getStatusCode() != HttpStatus.SC_OK) {
      Error error = JsonUtils.fromJson(responseEntity.getBody(), Error.class);
      throw new SubmittedException(StringUtils.isEmpty(error.getMessage()) ? responseEntity.getBody() : error.getMessage())
        .setStatus(responseEntity.getStatusCode());
    } else {
      return JsonUtils.fromJson(responseEntity.getBody(), AutomationTestResponse.class);
    }
  }
}
