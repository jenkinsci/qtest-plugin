package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import hudson.model.BuildListener;
import hudson.model.TaskListener;

import java.util.List;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public class JunitSubmitterRequest {

  private List<AutomationTestResult> testResults;
  private String buildNumber;
  private String buildPath;
  private TaskListener listener;
  private String qTestURL;
  private String apiKey;
  private String jenkinsProjectName;
  private Boolean submitToExistingContainer;
  private String jenkinsServerURL;
  private Long moduleID;
  private Long projectID;
  private Long configurationID;
  private Long containerID;
  private String containerType;
  private Long environmentID;



  private Long environmentParentID;

  public Boolean getCreateNewTestRunsEveryBuildDate() {
    return createNewTestRunsEveryBuildDate;
  }

  public void setCreateNewTestRunsEveryBuildDate(Boolean createNewTestRunsEveryBuildDate) {
    this.createNewTestRunsEveryBuildDate = createNewTestRunsEveryBuildDate;
  }

  private Boolean createNewTestRunsEveryBuildDate;

  public List<AutomationTestResult> getTestResults() {
    return testResults;
  }

  public JunitSubmitterRequest setTestResults(List<AutomationTestResult> testResults) {
    this.testResults = testResults;
    return this;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public JunitSubmitterRequest setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
    return this;
  }

  public String getBuildPath() {
    return buildPath;
  }

  public JunitSubmitterRequest setBuildPath(String buildPath) {
    this.buildPath = buildPath;
    return this;
  }

  public TaskListener getListener() {
    return listener;
  }

  public JunitSubmitterRequest setListener(TaskListener listener) {
    this.listener = listener;
    return this;
  }

  public String getqTestURL() {
    return qTestURL;
  }

  public JunitSubmitterRequest setqTestURL(String qTestURL) {
    this.qTestURL = qTestURL;
    return this;
  }

  public String getApiKey() {
    return apiKey;
  }

  public JunitSubmitterRequest setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public String getJenkinsProjectName() {
    return jenkinsProjectName;
  }

  public JunitSubmitterRequest setJenkinsProjectName(String jenkinsProjectName) {
    this.jenkinsProjectName = jenkinsProjectName;
    return this;
  }

  public Boolean getSubmitToExistingContainer() {
    return submitToExistingContainer;
  }

  public JunitSubmitterRequest setSubmitToExistingContainer(Boolean submitToExistingContainer) {
    this.submitToExistingContainer = submitToExistingContainer;
    return this;
  }

  public String getJenkinsServerURL() {
    return jenkinsServerURL;
  }

  public JunitSubmitterRequest setJenkinsServerURL(String jenkinsServerURL) {
    this.jenkinsServerURL = jenkinsServerURL;
    return this;
  }

  public Long getModuleID() {
    return moduleID;
  }

  public JunitSubmitterRequest setModuleID(Long moduleID) {
    this.moduleID = moduleID;
    return this;
  }

  public Long getProjectID() {
    return projectID;
  }

  public JunitSubmitterRequest setProjectID(Long projectID) {
    this.projectID = projectID;
    return this;
  }

  public Long getConfigurationID() {
    return configurationID;
  }

  public JunitSubmitterRequest setConfigurationID(Long configurationID) {
    this.configurationID = configurationID;
    return this;
  }

  public Long getContainerID() {
    return containerID;
  }

  public JunitSubmitterRequest setContainerID(Long containerID) {
    this.containerID = containerID;
    return this;
  }

  public String getContainerType() {
    return containerType;
  }

  public JunitSubmitterRequest setContainerType(String containerType) {
    this.containerType = containerType;
    return this;
  }
  public Long getEnvironmentID() {
    return environmentID;
  }

  public void setEnvironmentID(Long environmentID) {
    this.environmentID = environmentID;
  }

  public Long getEnvironmentParentID() {
    return environmentParentID;
  }

  public void setEnvironmentParentID(Long environmentParentID) {
    this.environmentParentID = environmentParentID;
  }
}