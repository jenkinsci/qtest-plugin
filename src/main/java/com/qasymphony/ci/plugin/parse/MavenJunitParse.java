package com.qasymphony.ci.plugin.parse;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.TestResult;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

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
    List<TestResult> testResults = new ArrayList<TestResult>();
    
    testResults.add(jUnitParser.parseResult(testResultLocation, build, build.getWorkspace(), launcher, listener));


    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());
    
    return CommonParsingUtils.toAutomationTestResults(testResults, gregorianCalendar.getTime(), build.getTime());
  }


  @Override
  public List<AutomationTestResult> parse() throws Exception {
    return parse(TEST_RESULT_LOCATIONS);
  }
}
