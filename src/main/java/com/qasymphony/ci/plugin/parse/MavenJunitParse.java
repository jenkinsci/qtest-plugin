/**
 * 
 */
package com.qasymphony.ci.plugin.parse;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.CaseResult.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.qasymphony.ci.plugin.model.AutomationTestLog;
import com.qasymphony.ci.plugin.model.AutomationTestResult;

/**
 * @author anpham
 *
 */
public class MavenJunitParse implements TestResultParse {
  public static final String TEST_RESULT_LOCATIONS = "/target/surefire-reports/*.xml";
  @SuppressWarnings("rawtypes")
  private AbstractBuild build;
  private Launcher launcher;
  private BuildListener listener;
  
  @SuppressWarnings("rawtypes")
  public MavenJunitParse(AbstractBuild build, Launcher launcher, BuildListener listener) {
    this.build = build;
    this.launcher = launcher;
    this.listener = listener;
  }

  @Override
  public List<AutomationTestResult> parse() throws Exception {
 JUnitParser jUnitParser = new JUnitParser(true);
    
    List<AutomationTestResult> automationTestResults = new ArrayList<AutomationTestResult>();
    AutomationTestResult automationTestResult = null;
    AutomationTestLog automationTestLog = null;
    List<AutomationTestLog> automationTestLogs = null;

    int testlogOrder = 1;
    Date current = new Date();
    
    TestResult testResult = jUnitParser.parseResult(TEST_RESULT_LOCATIONS, build, build.getWorkspace(), launcher, listener);
    
    for(SuiteResult suite: testResult.getSuites()){
      automationTestLogs = new ArrayList<AutomationTestLog>();
      
      automationTestResult = new AutomationTestResult();
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
    
    return automationTestResults;
  }

}
