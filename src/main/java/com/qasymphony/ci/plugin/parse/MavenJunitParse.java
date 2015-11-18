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
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.qasymphony.ci.plugin.model.AutomationAttachment;
import com.qasymphony.ci.plugin.model.AutomationTestLog;
import com.qasymphony.ci.plugin.model.AutomationTestResult;

/**
 * @author anpham
 */
public class MavenJunitParse implements TestResultParse {
  public static final String TEST_RESULT_LOCATIONS = "target/surefire-reports/*.xml";
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

  public List<AutomationTestResult> parse(String testResultLocation) throws Exception {
    JUnitParser jUnitParser = new JUnitParser(true);
    HashMap<String, AutomationTestResult> automationTestResultMap = new HashMap<String, AutomationTestResult>();
    
    AutomationTestResult automationTestResult = null;
    AutomationTestLog automationTestLog = null;

    int testlogOrder = 1;
    Date current = new Date();

    TestResult testResult = jUnitParser.parseResult(testResultLocation, build, build.getWorkspace(), launcher, listener);
    AutomationAttachment attachment = null;

    for (SuiteResult suite : testResult.getSuites()) {
      if (suite.getCases() == null) {
        continue;
      } else {
        for (CaseResult caseResult : suite.getCases()) {
          if(automationTestResultMap.containsKey(caseResult.getClassName())){
            automationTestResult = automationTestResultMap.get(caseResult.getClassName());
            if(caseResult.isFailed()){
              automationTestResult.setStatus(Status.FAILED.toString());
            }
          }else {
            automationTestResult = new AutomationTestResult();
            automationTestResult.setName(caseResult.getClassName());
            automationTestResult.setAutomationContent(caseResult.getClassName());
            automationTestResult.setExecutedEndDate(current);
            automationTestResult.setExecutedStartDate(current);
            automationTestResult.setStatus(caseResult.isPassed() ? Status.PASSED.toString() : Status.FAILED.toString());
            automationTestResult.setTestLogs(new ArrayList<AutomationTestLog>());
            automationTestResult.setAttachments(new ArrayList<AutomationAttachment>());
            
            automationTestResultMap.put(caseResult.getClassName(), automationTestResult);
          }
          automationTestLog = new AutomationTestLog();
          automationTestLog.setDescription(caseResult.getName());
          automationTestLog.setExpectedResult(caseResult.getName());
          automationTestLog.setOrder(testlogOrder);
          automationTestLog.setStatus(caseResult.getStatus().toString());

          automationTestResult.getTestLogs().add(automationTestLog);
          
          if(caseResult.isFailed()){
            attachment = new AutomationAttachment();
            attachment.setName(caseResult.getName().concat(".txt"));
            attachment.setContentType("text/plain");
            attachment.setData(Base64.encodeBase64String(caseResult.getErrorStackTrace().getBytes()));
            automationTestResult.getAttachments().add(attachment);
          }

          testlogOrder++;
        }
      }
    }
    
    return new ArrayList<>(automationTestResultMap.values());
  }

  @Override
  public List<AutomationTestResult> parse() throws Exception {
    return parse(TEST_RESULT_LOCATIONS);
  }
}
