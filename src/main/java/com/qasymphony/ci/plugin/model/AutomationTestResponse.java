package com.qasymphony.ci.plugin.model;

/**
 * @author anpham
 */
public class AutomationTestResponse {
  private String testSuiteName;
  private long testSuiteId;
  private int totalTestCases;
  private int totalTestLogs;

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public void setTestSuiteName(String testSuiteName) {
    this.testSuiteName = testSuiteName;
  }

  public long getTestSuiteId() {
    return testSuiteId;
  }

  public void setTestSuiteId(long testSuiteId) {
    this.testSuiteId = testSuiteId;
  }

  public int getTotalTestCases() {
    return totalTestCases;
  }

  public void setTotalTestCases(int totalTestCases) {
    this.totalTestCases = totalTestCases;
  }

  public int getTotalTestLogs() {
    return totalTestLogs;
  }

  public void setTotalTestLogs(int totalTestLogs) {
    this.totalTestLogs = totalTestLogs;
  }
}
