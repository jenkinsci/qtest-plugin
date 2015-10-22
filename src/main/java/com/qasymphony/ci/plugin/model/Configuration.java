/**
 * 
 */
package com.qasymphony.ci.plugin.model;

/**
 * @author anpham
 * 
 */
public class Configuration {
  private String url;
  private String appSecretKey;
  private long projectId;
  private String projectName;
  private long releaseId;
  private String releaseName;
  private String environment;
  private long testSuiteId;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getAppSecretKey() {
    return appSecretKey;
  }

  public void setAppSecretKey(String appSecretKey) {
    this.appSecretKey = appSecretKey;
  }

  public long getProjectId() {
    return projectId;
  }

  public void setProjectId(long projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public long getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(long releaseId) {
    this.releaseId = releaseId;
  }

  public String getReleaseName() {
    return releaseName;
  }

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public long getTestSuiteId() {
    return testSuiteId;
  }

  public void setTestSuiteId(long testSuiteId) {
    this.testSuiteId = testSuiteId;
  }

}
