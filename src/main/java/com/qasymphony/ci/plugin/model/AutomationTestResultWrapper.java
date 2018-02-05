package com.qasymphony.ci.plugin.model;

import java.util.List;

/**
 * @author anpham
 * 
 */
public class AutomationTestResultWrapper {
  private String buildNumber;
  private String buildPath;

  // V3.1
  private Long test_suite;

  public Long getParent_module() {
    return parent_module;
  }

  public Boolean skipCreatingAutomationModule;

  public Boolean getSkipCreatingAutomationModule() {
    return skipCreatingAutomationModule;
  }

  public void setSkipCreatingAutomationModule(Boolean skipCreatingAutomationModule) {
    this.skipCreatingAutomationModule = skipCreatingAutomationModule;
  }

  public void setParent_module(Long parent_module) {
    this.parent_module = parent_module;
  }

  private Long parent_module;
  private List<AutomationTestResult> test_logs;

  public Long getTest_suite() {
    return test_suite;
  }

  public void setTest_suite(Long test_suite) {
    this.test_suite = test_suite;
  }

  public List<AutomationTestResult> getTest_logs() {
    return test_logs;
  }

  public void setTest_logs(List<AutomationTestResult> test_logs) {
    this.test_logs = test_logs;
  }

  //~V3.1

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

  //V3
  private List<AutomationTestResult> testResults;

  public List<AutomationTestResult> getTestResults() {
    return testResults;
  }

  public void setTestResults(List<AutomationTestResult> testResults) {
    this.testResults = testResults;
  }
  //~V3
}
