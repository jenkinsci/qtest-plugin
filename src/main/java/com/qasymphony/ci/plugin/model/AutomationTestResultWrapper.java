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
  private String buildId;
  private String buildPath;

  private List<AutomationTestResult> testResults;

  public String getBuildId() {
    return buildId;
  }

  public void setBuildId(String buildId) {
    this.buildId = buildId;
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
