package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;

import java.util.List;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public class JunitSubmitterRequest {
  private Configuration configuration;
  private List<AutomationTestResult> testResults;
  private String buildNumber;
  private String buildPath;

  public Configuration getConfiguration() {
    return configuration;
  }

  public JunitSubmitterRequest setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }

  public List<AutomationTestResult> getTestResults() {
    return testResults;
  }

  public JunitSubmitterRequest setTestResults(List<AutomationTestResult> testResults) {
    this.testResults = testResults;
    return this;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public JunitSubmitterRequest setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
    return this;
  }

  public String getBuildPath() {
    return buildPath;
  }

  public JunitSubmitterRequest setBuildPath(String buildPath) {
    this.buildPath = buildPath;
    return this;
  }

}