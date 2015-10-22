package com.qasymphony.ci.plugin.model;

/**
 * @author trongle
 * @version 10/22/2015 9:37 PM trongle $
 * @since 1.0
 */
public class SubmitResult {
  private Long buildNumber;
  private String statusBuild;
  private String testSuiteName;
  private String submitStatus;

  public Long getBuildNumber() {
    return buildNumber;
  }

  public SubmitResult setBuildNumber(Long buildNumber) {
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
}
