/**
 * 
 */
package com.qasymphony.ci.plugin;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.Error;
import com.qasymphony.ci.plugin.parse.TestResultParse;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
/**
 * @author anpham
 *
 */
public class AutomationTestService {
  private static String AUTO_TEST_LOG_ENDPOINT = "api/v3/projects/{0}/test-runs/{1}/auto-test-logs/ci/jenkins";
  
  public static List<AutomationTestResult> push(TestResultParse testResultParse, Configuration configuration, Map<String, String> headers) throws Exception {
    List<AutomationTestResult> automationTestResults = testResultParse.parse();
    
    if(automationTestResults.size() > 0){
      ResponseEntity responseEntity = HttpClientUtils.post(configuration.getUrl().concat("/")
          .concat(MessageFormat.format(AUTO_TEST_LOG_ENDPOINT, new Object[]{configuration.getProjectId(), 0}))
          , headers, JsonUtils.toJson(automationTestResults));
      
      if(responseEntity.getStatusCode() != HttpStatus.SC_OK){
        Error error = JsonUtils.fromJson(responseEntity.getBody(), Error.class);
        throw new Exception(error.getMessage());
      }else {
        return null;
      }
    }
    
    return automationTestResults;
  }
}
