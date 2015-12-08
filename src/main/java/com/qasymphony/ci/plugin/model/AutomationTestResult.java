package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import hudson.tasks.junit.CaseResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author anpham
 */
public class AutomationTestResult {
  @JsonProperty("exe_start_date")
  private Date executedStartDate;
  @JsonProperty("exe_end_date")
  private Date executedEndDate;
  @JsonProperty("automation_content")
  private String automationContent;
  private String status;
  private String name;
  @JsonProperty("test_step_logs")
  private List<AutomationTestLog> testLogs;
  private List<AutomationAttachment> attachments;
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
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<AutomationTestLog> getTestLogs() {
    return testLogs;
  }

  public void setTestLogs(List<AutomationTestLog> testLogs) {
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

  /**
   * Add testLog and resolve status of testResult
   * @param automationTestLog
   * @return
   */
  public AutomationTestLog addTestLog(AutomationTestLog automationTestLog) {
    testLogs.add(automationTestLog);
    if (CaseResult.Status.FAILED.toString().equalsIgnoreCase(automationTestLog.getStatus())) {
      totalFailedTestSteps += 1;
    }
    if (CaseResult.Status.SKIPPED.toString().equalsIgnoreCase(automationTestLog.getStatus())) {
      totalSkippedTestSteps += 1;
    }
    if (CaseResult.Status.PASSED.toString().equalsIgnoreCase(automationTestLog.getStatus())) {
      totalSuccessTestSteps += 1;
    }
    this.setStatus(automationTestLog.getStatus());
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
    return automationTestLog;
  }
}
