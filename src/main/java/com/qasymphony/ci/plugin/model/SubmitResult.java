package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author trongle
 * @version 10/22/2015 9:37 PM trongle $
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitResult {
  private int buildNumber;
  private String statusBuild;
  private String testSuiteName;
  private String submitStatus;
  private int numberTestResult;
  private int numberTestRun;

  public int getBuildNumber() {
    return buildNumber;
  }

  public SubmitResult setBuildNumber(int buildNumber) {
    this.buildNumber = buildNumber;
    return this;
  }

  public String getStatusBuild() {
    return statusBuild;
  }

  public SubmitResult setStatusBuild(String statusBuild) {
    this.statusBuild = statusBuild;
    return this;
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public SubmitResult setTestSuiteName(String testSuiteName) {
    this.testSuiteName = testSuiteName;
    return this;
  }

  public String getSubmitStatus() {
    return submitStatus;
  }

  public SubmitResult setSubmitStatus(String submitStatus) {
    this.submitStatus = submitStatus;
    return this;
  }

  public int getNumberTestResult() {
    return numberTestResult;
  }

  public SubmitResult setNumberTestResult(int numberTestResult) {
    this.numberTestResult = numberTestResult;
    return this;
  }

  public int getNumberTestRun() {
    return numberTestRun;
  }

  public SubmitResult setNumberTestRun(int numberTestRun) {
    this.numberTestRun = numberTestRun;
    return this;
  }
}
