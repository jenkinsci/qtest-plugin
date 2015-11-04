/**
 * 
 */
package com.qasymphony.ci.plugin.model;

import java.util.List;

/**
 * @author anpham
 * 
 */
public class AutomationTestResultWrapper {
  private String buildNumber;
  private String buildPath;

  private List<AutomationTestResult> testResults;

  public String getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
  }

  public String getBuildPath() {
    return buildPath;
  }

  public void setBuildPath(String buildPath) {
    this.buildPath = buildPath;
  }

  public List<AutomationTestResult> getTestResults() {
    return testResults;
  }

  public void setTestResults(List<AutomationTestResult> testResults) {
    this.testResults = testResults;
  }

}
