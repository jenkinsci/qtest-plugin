package com.qasymphony.ci.plugin.submitter;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.qasymphony.ci.plugin.model.AutomationTestLog;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.JsonUtils;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class JunitQtestSubmitterImpl implements JunitSubmitter {
  @Override public JunitSubmitterResult submit(JunitSubmitterRequest junitSubmitterRequest) {
    //TODO: submit to qTest and receive test suite id
    JunitSubmitterResult junitSubmitterResult = new JunitSubmitterResult();
    junitSubmitterResult.setTestSuiteId(System.currentTimeMillis());
    return junitSubmitterResult;
  }

  @Override public void storeSubmitterResult() {

  }

  @Override
  public void push(String testResultLocations, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
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
      automationTestResult.setAutomationContent(suite.getName());
      automationTestResult.setExecutedEndDate(current);
      automationTestResult.setExecutedStartDate(current);
      automationTestResult.setStatus(null);
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
    //TODO make a request to push automationTestResult to QTest via API
  }
}
