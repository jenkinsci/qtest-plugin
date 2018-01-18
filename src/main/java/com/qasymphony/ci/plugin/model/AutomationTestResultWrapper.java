package com.qasymphony.ci.plugin.model;

import java.util.List;

/**
 * @author anpham
 * 
 */
public class AutomationTestResultWrapper {
  private String buildNumber;
  private String buildPath;

  private Long test_suite;

  public Long getParent_module() {
    return parent_module;
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

  public String getExecution_date() {
    return execution_date;
  }

  public void setExecution_date(String execution_date) {
    this.execution_date = execution_date;
  }

  private String execution_date;

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



}
