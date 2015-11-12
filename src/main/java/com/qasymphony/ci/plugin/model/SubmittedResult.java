package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author trongle
 * @version 10/22/2015 9:37 PM trongle $
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmittedResult {
  private int buildNumber;
  private String statusBuild;
  private Long testSuiteId;
  private String testSuiteName;
  private String submitStatus;
  private int numberTestResult;
  private int numberTestLog;
  private String testSuiteLink;

  public int getBuildNumber() {
    return buildNumber;
  }

  public SubmittedResult setBuildNumber(int buildNumber) {
    this.buildNumber = buildNumber;
    return this;
  }

  public String getStatusBuild() {
    return statusBuild;
  }

  public SubmittedResult setStatusBuild(String statusBuild) {
    this.statusBuild = statusBuild;
    return this;
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public SubmittedResult setTestSuiteName(String testSuiteName) {
    this.testSuiteName = testSuiteName;
    return this;
  }

  public String getSubmitStatus() {
    return submitStatus;
  }

  public SubmittedResult setSubmitStatus(String submitStatus) {
    this.submitStatus = submitStatus;
    return this;
  }

  public int getNumberTestResult() {
    return numberTestResult;
  }

  public SubmittedResult setNumberTestResult(int numberTestResult) {
    this.numberTestResult = numberTestResult;
    return this;
  }

  public int getNumberTestLog() {
    return numberTestLog;
  }

  public SubmittedResult setNumberTestLog(int numberTestLog) {
    this.numberTestLog = numberTestLog;
    return this;
  }

  public Long getTestSuiteId() {
    return testSuiteId;
  }

  public SubmittedResult setTestSuiteId(Long testSuiteId) {
    this.testSuiteId = testSuiteId;
    return this;
  }

  public String getTestSuiteLink() {
    return testSuiteLink;
  }

  public SubmittedResult setTestSuiteLink(String testSuiteLink) {
    this.testSuiteLink = testSuiteLink;
    return this;
  }
}
