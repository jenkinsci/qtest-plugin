/**
 * 
 */
package com.qasymphony.ci.plugin;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.CaseResult.Status;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;

import com.qasymphony.ci.plugin.model.AutomationTestLog;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.Error;
import com.qasymphony.ci.plugin.model.Module;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
/**
 * @author anpham
 *
 */
public class AutomationTestService {
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static String AUTO_TEST_LOG_ENDPOINT = "api/v3/projects/{0}/test-runs/{1}/auto-test-logs/ci/jenkins";
  private static String CI_MODULE_ENDPOINT = "api/v3/projects/{0}/modules/ci/jenkins";
  
  public static Configuration createModule(String name, Configuration configuration) throws Exception{
    Module module = new Module();
    module.setName(name);
    
    Map<String, String> headers = new HashMap<>();
    headers.put(HEADER_AUTHORIZATION, configuration.getAppSecretKey());
    
    String endpoint = MessageFormat.format(CI_MODULE_ENDPOINT, new Object[]{configuration.getProjectId()});
    if(configuration.getModuleId() > 0){
      endpoint = endpoint.concat("/").concat(String.valueOf(configuration.getModuleId()));
    }
    
    ResponseEntity responseEntity = HttpClientUtils.post(configuration.getUrl().concat("/").concat(endpoint)
        , headers, JsonUtils.toJson(module));
    
    if(responseEntity.getStatusCode() != HttpStatus.SC_OK){
      Error error = JsonUtils.fromJson(responseEntity.getBody(), Error.class);
      throw new Exception(error.getMessage());
    }
    module = JsonUtils.fromJson(responseEntity.getBody(), Module.class);
    
    configuration.setModuleId(module.getId());
    return configuration;
  }
  
  public static List<AutomationTestResult> push(String testResultLocations, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, Configuration configuration, Map<String, String> headers) throws Exception {
    JUnitParser jUnitParser = new JUnitParser(true);
    
    testResultLocations = (testResultLocations == null ? "/target/surefire-reports/*.xml" : testResultLocations);
    
    List<AutomationTestResult> automationTestResults = new ArrayList<AutomationTestResult>();
    AutomationTestResult automationTestResult = null;
    AutomationTestLog automationTestLog = null;
    List<AutomationTestLog> automationTestLogs = null;

    int testlogOrder = 1;
    Date current = new Date();
    
    TestResult testResult = jUnitParser.parseResult(testResultLocations, build, workspace, launcher, listener);
    
    for(SuiteResult suite: testResult.getSuites()){
      automationTestLogs = new ArrayList<AutomationTestLog>();
      
      automationTestResult = new AutomationTestResult();
      automationTestResult.setModuleId(configuration.getModuleId());
      automationTestResult.setReleaseId(configuration.getReleaseId());
      automationTestResult.setName(suite.getName());
      automationTestResult.setAutomationContent(suite.getName());
      automationTestResult.setExecutedEndDate(current);
      automationTestResult.setExecutedStartDate(current);
      automationTestResult.setStatus(testResult.isPassed() ? Status.PASSED.toString() : Status.FAILED.toString());
      automationTestResult.setTestLogs(automationTestLogs);
      
      if(suite.getCases() == null){
        continue;
      }else {
        for(CaseResult caseResult: suite.getCases()){
          automationTestLog = new AutomationTestLog();
          automationTestLog.setDescription(caseResult.getName());
          automationTestLog.setExpectedResult(caseResult.getName());
          automationTestLog.setOrder(testlogOrder);
          automationTestLog.setStatus(caseResult.getStatus().toString());
          
          automationTestLogs.add(automationTestLog);
          
          testlogOrder ++;
        }
      }
      
      automationTestResults.add(automationTestResult);
    }
    
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
