package com.qasymphony.ci.plugin.submitter;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public class JunitSubmitterResult {
  public static final String STATUS_SUCCESS = "Success";
  public static final String STATUS_CANCELED = "Canceled";
  public static final String STATUS_FAILED = "Failed";
  private Long testSuiteId;
  private String testSuiteName;
  private String submittedStatus;
  private Integer numberOfTestRun;
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

  public Integer getNumberOfTestRun() {
    return numberOfTestRun;
  }

  public JunitSubmitterResult setNumberOfTestRun(Integer numberOfTestRun) {
    this.numberOfTestRun = numberOfTestRun;
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
