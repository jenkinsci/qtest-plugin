/**
 * 
 */
package com.qasymphony.ci.plugin.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author anpham
 * 
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

}
