package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import hudson.tasks.junit.CaseResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author anpham
 */
public class AutomationTestResult {
  @JsonProperty("build_number")
  private String buildNumber;

  @JsonProperty("build_url")
  private String buildURL;

  @JsonProperty("exe_start_date")
  private Date executedStartDate;
  @JsonProperty("exe_end_date")
  private Date executedEndDate;
  @JsonProperty("automation_content")
  private String automationContent;
  private String status;
  private String name;

  private Integer order;
  @JsonProperty("test_step_logs")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<AutomationTestStepLog> testLogs;
  private List<AutomationAttachment> attachments = new ArrayList<>();
  /**
   * total failed testStep
   */
  private int totalFailedTestSteps = 0;
  /**
   * total skipped testStep
   */
  private int totalSkippedTestSteps = 0;
  /**
   * total success testStep
   */
  private int totalSuccessTestSteps = 0;
  private int currentOrder = 0;

  public AutomationTestResult() {
    testLogs = new ArrayList<>();
  }

  public Date getExecutedStartDate() {
    return executedStartDate;
  }

  public void setExecutedStartDate(Date executedStartDate) {
    this.executedStartDate = executedStartDate;
  }

  public Date getExecutedEndDate() {
    return executedEndDate;
  }

  public void setExecutedEndDate(Date executedEndDate) {
    this.executedEndDate = executedEndDate;
  }

  public String getAutomationContent() {
    return automationContent;
  }

  public void setAutomationContent(String automationContent) {
    this.automationContent = automationContent;
    this.setName(automationContent);
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<AutomationTestStepLog> getTestLogs() {
    return testLogs;
  }

  public void setTestLogs(List<AutomationTestStepLog> testLogs) {
    this.testLogs = testLogs;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<AutomationAttachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<AutomationAttachment> attachments) {
    this.attachments = attachments;
  }

  public List<AutomationAttachment> addAttachment(AutomationAttachment attachment) {
    attachments.add(attachment);
    return attachments;
  }
  public String getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
  }

  public String getBuildURL() {
    return buildURL;
  }

  public void setBuildURL(String buildURL) {
    this.buildURL = buildURL;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }


  /**
   * Add testLog and resolve status of testResult
   *
   * @param automationTestStepLog automationTestStepLog
   * @return {@link AutomationTestStepLog}
   * @see "http://javadoc.jenkins-ci.org/hudson/tasks/junit/CaseResult.Status.html"
   */
  public AutomationTestStepLog addTestStepLog(AutomationTestStepLog automationTestStepLog, boolean isOverwriteExistingTestSteps) {
    automationTestStepLog.setOrder(currentOrder++);
    if (isOverwriteExistingTestSteps) {
      testLogs.add(automationTestStepLog);
    }
    String status = automationTestStepLog.getStatus();
    //regression the same as failed
    if (CaseResult.Status.FAILED.toString().equalsIgnoreCase(status) ||
      CaseResult.Status.REGRESSION.toString().equalsIgnoreCase(status)) {
      totalFailedTestSteps += 1;
    }
    if (CaseResult.Status.SKIPPED.toString().equalsIgnoreCase(status)) {
      totalSkippedTestSteps += 1;
    }
    //fixed the same as passed
    if (CaseResult.Status.PASSED.toString().equalsIgnoreCase(status) ||
      CaseResult.Status.FIXED.toString().equalsIgnoreCase(status)) {
      totalSuccessTestSteps += 1;
    }
    this.setStatus(status);
    if (totalFailedTestSteps >= 1) {
      //if there is one failed testStep, we mark testLog as failed
      this.setStatus(CaseResult.Status.FAILED.toString());
    } else {
      if (totalSuccessTestSteps >= 1) {
        //if there is one success testStep, we mark testLog as success
        this.setStatus(CaseResult.Status.PASSED.toString());
      } else if (totalSkippedTestSteps >= 1) {
        //if all of test is skipped, we mark testLog as skipped
        this.setStatus(CaseResult.Status.SKIPPED.toString());
      }
    }
    return automationTestStepLog;
  }
}
