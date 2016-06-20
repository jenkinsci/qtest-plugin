package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
   * @param request
   * @param testResultLocation
   * @return
   * @throws Exception
   */
  public List<AutomationTestResult> parse(ParseRequest request, String testResultLocation) throws Exception {
    JUnitParser jUnitParser = new JUnitParser(true);
    List<TestResult> testResults = new ArrayList<>();
    AbstractBuild build = request.getBuild();
    Launcher launcher = request.getLauncher();
    BuildListener listener = request.getListener();
    testResults.add(jUnitParser.parseResult(testResultLocation, build, build.getWorkspace(), launcher, listener));

    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());

    return CommonParsingUtils.toAutomationTestResults(request.getConfiguration().getEachMethodAsTestCase(), testResults, gregorianCalendar.getTime(), build.getTime());
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  @Override
  public List<AutomationTestResult> parse(ParseRequest request)
    throws Exception {
    Configuration configuration = request.getConfiguration();
    BuildListener listener = request.getListener();

    LoggerUtils.formatInfo(listener.getLogger(), "Scan with test result location: %s", configuration.getResultPattern());
    //if configured with result location pattern
    return parse(request, configuration.getResultPattern());
  }
}
