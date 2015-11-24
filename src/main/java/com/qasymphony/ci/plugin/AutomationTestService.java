/**
 *
 */
package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.*;
import com.qasymphony.ci.plugin.model.Error;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author anpham
 */
public class AutomationTestService {
  public static String AUTO_TEST_LOG_ENDPOINT = "%s/api/v3/projects/%s/test-runs/%s/auto-test-logs/ci/%s";

  public static AutomationTestResponse push(String buildNumber, String buildPath, List<AutomationTestResult> testResults, Configuration configuration, Map<String, String> headers)
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
      throw new SubmittedException(e.getMessage(), responseEntity.getStatusCode());
    }

    if (responseEntity.getStatusCode() != HttpStatus.SC_OK) {
      Error error = JsonUtils.fromJson(responseEntity.getBody(), Error.class);
      throw new SubmittedException(
        StringUtils.isEmpty(error.getMessage()) ? responseEntity.getBody() : error.getMessage(), responseEntity.getStatusCode());
    } else {
      return JsonUtils.fromJson(responseEntity.getBody(), AutomationTestResponse.class);
    }
  }
}
