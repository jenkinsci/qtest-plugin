package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.TestResult;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * scan with configured test results location pattern
 *
 * @author anpham
 */
public class PatternScanParser implements TestResultParser {
  /**
   * Read test results with test result location pattern
   *
   * @param request            request
   * @param testResultLocation testResultLocation
   * @return a list of {@link AutomationTestResult}
   * @throws Exception Exception
   */
  public List<AutomationTestResult> parse(ParseRequest request, String testResultLocation) throws Exception {
    JUnitParser jUnitParser = new JUnitParser(true);
    Run<?,?> build = request.getBuild();
    Launcher launcher = request.getLauncher();
    TaskListener listener = request.getListener();
    List<TestResult> testResults = new ArrayList<>();
    testResults.add(jUnitParser.parseResult(testResultLocation, build, request.getWorkSpace(), launcher, listener));

    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());

    return CommonParsingUtils.toAutomationTestResults(request, testResults, gregorianCalendar.getTime());
  }

  /**
   * @param request request
   * @return a list of {@link AutomationTestResult}
   * @throws Exception Exception
   */
  @Override
  public List<AutomationTestResult> parse(ParseRequest request)
    throws Exception {
    TaskListener listener = request.getListener();

    LoggerUtils.formatInfo(listener.getLogger(), "Scan with test result location: %s", request.getParseTestResultPattern());
    //if configured with result location pattern
    return parse(request, request.getParseTestResultPattern());
  }
}
