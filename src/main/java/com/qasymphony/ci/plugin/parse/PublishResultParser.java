package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.model.AbstractBuild;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author anpham
 */
public class PublishResultParser implements TestResultParser {

  @Override
  public List<AutomationTestResult> parse(ParseRequest request) throws Exception {
    List<TestResult> testResults = new ArrayList<>();
    AbstractBuild build = request.getBuild();
    TestResultAction resultAction = build.getAction(TestResultAction.class);
    if (resultAction != null) {
      testResults.add(resultAction.getResult());
    } else {
      AggregatedTestResultAction aggregatedTestResultAction = build.getAction(AggregatedTestResultAction.class);
      if (aggregatedTestResultAction != null) {
        List<ChildReport> childReports = aggregatedTestResultAction.getResult();
        if (childReports != null) {
          for (ChildReport childReport : childReports) {
            if (childReport.result instanceof TestResult) {
              testResults.add((TestResult) childReport.result);
            }
          }
        }
      } else {
        LoggerUtils.formatWarn(request.getListener().getLogger(), "No testResult action was added to project.");
      }
    }

    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());

    return CommonParsingUtils.toAutomationTestResults(testResults, gregorianCalendar.getTime(), build.getTime());
  }
}
