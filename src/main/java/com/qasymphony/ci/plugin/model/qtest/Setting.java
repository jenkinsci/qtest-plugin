package com.qasymphony.ci.plugin.model.qtest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author trongle
 * @version 10/28/2015 11:22 AM trongle $
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Setting {
  @JsonProperty("ci_type")
  public String ciType;

  @JsonProperty("id")
  private Long id;

  @JsonProperty("ci_server")
  private String jenkinsServer;

  @JsonProperty("ci_project")
  private String jenkinsProjectName;

  @JsonProperty("project_id")
  private Long projectId;

  @JsonProperty("release_id")
  private Long releaseId;

  @JsonProperty("test_suite_id")
  private Long testSuiteId;

  @JsonProperty("module_id")
  private Long moduleId;

  @JsonProperty("environment_id")
  private Long environmentId;

  @JsonProperty("ciid")
  private String hmac;

  public String getCiType() {
    return ciType;
  }

  public Setting setCiType(String ciType) {
    this.ciType = ciType;
    return this;
  }

  public Long getId() {
    return id;
  }

  public Setting setId(Long id) {
    this.id = id;
    return this;
  }

  public String getJenkinsServer() {
    return jenkinsServer;
  }

  public Setting setJenkinsServer(String jenkinsServer) {
    this.jenkinsServer = jenkinsServer;
    return this;
  }

  public String getJenkinsProjectName() {
    return jenkinsProjectName;
  }

  public Setting setJenkinsProjectName(String jenkinsProjectName) {
    this.jenkinsProjectName = jenkinsProjectName;
    return this;
  }

  public Long getProjectId() {
    return projectId;
  }

  public Setting setProjectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  public Long getReleaseId() {
    return releaseId;
  }

  public Setting setReleaseId(Long releaseId) {
    this.releaseId = releaseId;
    return this;
  }

  public Long getTestSuiteId() {
    return testSuiteId;
  }

  public Setting setTestSuiteId(Long testSuiteId) {
    this.testSuiteId = testSuiteId;
    return this;
  }

  public Long getModuleId() {
    return moduleId;
  }

  public Setting setModuleId(Long moduleId) {
    this.moduleId = moduleId;
    return this;
  }

  public Long getEnvironmentId() {
    return environmentId;
  }

  public Setting setEnvironmentId(Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  public String getHmac() {
    return hmac;
  }

  public Setting setHmac(String hmac) {
    this.hmac = hmac;
    return this;
  }
}
