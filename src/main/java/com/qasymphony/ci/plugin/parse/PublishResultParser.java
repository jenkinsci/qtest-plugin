/**
 * 
 */
package com.qasymphony.ci.plugin.parse;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import com.qasymphony.ci.plugin.model.AutomationTestResult;

/**
 * @author anpham
 *
 */
public class PublishResultParser implements TestResultParse { 
  @SuppressWarnings("rawtypes")
  private AbstractBuild build;
  
  @SuppressWarnings("rawtypes")
  public PublishResultParser(AbstractBuild build) {
    this.build = build;
  }

  @Override
  public List<AutomationTestResult> parse() throws Exception {
    List<TestResult> testResults = new ArrayList<TestResult>();
    
    TestResultAction resultAction = build.getAction(TestResultAction.class);
    if(resultAction != null){
      testResults.add(resultAction.getResult());
    }else {
      AggregatedTestResultAction aggregatedTestResultAction = build.getAction(AggregatedTestResultAction.class);
      if(aggregatedTestResultAction != null){
        List<ChildReport> childReports = aggregatedTestResultAction.getResult();
        if(childReports != null){
          for(ChildReport childReport: childReports){
            if(childReport.result instanceof TestResult){
              testResults.add((TestResult) childReport.result);
            }
          }
        }
      }
    }
    
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());
    
    return CommonParsingUtils.toAutomationTestResults(testResults, gregorianCalendar.getTime(), build.getTime());
  }

}
