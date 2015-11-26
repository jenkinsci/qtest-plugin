package com.qasymphony.ci.plugin.submitter;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public class JunitSubmitterResult {
  public static final String STATUS_SUCCESS = "Complete";
  public static final String STATUS_CANCELED = "Canceled";
  public static final String STATUS_FAILED = "Failed";
  public static final String STATUS_SKIPPED = "Skipped";
  private Long testSuiteId;
  private String testSuiteName;
  private String submittedStatus;
  private Integer numberOfTestLog;
  private Integer numberOfTestResult;

  public Long getTestSuiteId() {
    return testSuiteId;
  }

  public JunitSubmitterResult setTestSuiteId(Long testSuiteId) {
    this.testSuiteId = testSuiteId;
    return this;
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public JunitSubmitterResult setTestSuiteName(String testSuiteName) {
    this.testSuiteName = testSuiteName;
    return this;
  }

  public String getSubmittedStatus() {
    return submittedStatus;
  }

  public JunitSubmitterResult setSubmittedStatus(String submittedStatus) {
    this.submittedStatus = submittedStatus;
    return this;
  }

  public Integer getNumberOfTestLog() {
    return numberOfTestLog;
  }

  public JunitSubmitterResult setNumberOfTestLog(Integer numberOfTestLog) {
    this.numberOfTestLog = numberOfTestLog;
    return this;
  }

  public Integer getNumberOfTestResult() {
    return numberOfTestResult;
  }

  public JunitSubmitterResult setNumberOfTestResult(Integer numberOfTestResult) {
    this.numberOfTestResult = numberOfTestResult;
    return this;
  }
}
