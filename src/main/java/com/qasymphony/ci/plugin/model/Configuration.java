package com.qasymphony.ci.plugin.model;

import com.qasymphony.ci.plugin.model.qtest.Setting;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author anpham
 */
public class Configuration extends AbstractDescribableImpl<Configuration> {
  private Long id;
  private String url;
  /**
   * Refresh token
   */
  private String appSecretKey;
  private long projectId;
  private String projectName;
  private long releaseId;
  private String releaseName;
  private long environmentId;
  private String environmentName;
  private long testSuiteId;
  private long moduleId;
  private String jenkinsServerUrl;
  private String jenkinsProjectName;

  private boolean submitToContainer;
  private boolean createNewTestRunsEveryBuild;
  private long containerId;
  private String containerType;
  /**
   * Read from testResult action from jenkins
   */
  private Boolean readFromJenkins;
  private String resultPattern;
  private Boolean eachMethodAsTestCase;

  public static Configuration newInstance() {
    return new Configuration(0L, "", "", 0, "", 0L, "", 0, "",
            0, 0, false, "", false, false, 0, "");
  }

  @DataBoundConstructor
  public Configuration(Long id, String url, String appSecretKey, long projectId,
                       String projectName, long releaseId, String releaseName, long environmentId,
                       String environmentName, long testSuiteId, long moduleId, Boolean readFromJenkins, String resultPattern,
                       Boolean submitToContainer, Boolean createNewTestRunsEveryBuild, long containerId, String containerType) {
    this.url = url;
    this.appSecretKey = appSecretKey;
    this.projectId = projectId;
    this.projectName = projectName;
    this.releaseId = releaseId;
    this.releaseName = releaseName;
    this.environmentId = environmentId;
    this.environmentName = environmentName;
    this.testSuiteId = testSuiteId;
    this.moduleId = moduleId;
    this.id = id;
    this.readFromJenkins = readFromJenkins;
    this.resultPattern = resultPattern;
    this.submitToContainer = submitToContainer;
    this.createNewTestRunsEveryBuild = createNewTestRunsEveryBuild;
    this.containerId = containerId;
    this.containerType = containerType;
  }

  public Long getId() {
    return id;
  }

  public Configuration setId(Long id) {
    this.id = id;
    return this;
  }

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

  public String getEnvironmentName() {
    return environmentName;
  }

  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }

  public long getEnvironmentId() {
    return environmentId;
  }

  public Configuration setEnvironmentId(long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  public long getTestSuiteId() {
    return testSuiteId;
  }

  public void setTestSuiteId(long testSuiteId) {
    this.testSuiteId = testSuiteId;
  }

  public long getModuleId() {
    return moduleId;
  }

  public void setModuleId(long moduleId) {
    this.moduleId = moduleId;
  }

  public String getJenkinsServerUrl() {
    return jenkinsServerUrl;
  }

  public Configuration setJenkinsServerUrl(String jenkinsServerUrl) {
    this.jenkinsServerUrl = jenkinsServerUrl;
    return this;
  }

  public String getJenkinsProjectName() {
    return jenkinsProjectName;
  }

  public Configuration setJenkinsProjectName(String jenkinsProjectName) {
    this.jenkinsProjectName = jenkinsProjectName;
    return this;
  }

  public Boolean getReadFromJenkins() {
    //new version: make default read by parse file
    return readFromJenkins == null ? true : readFromJenkins;
  }


  public Boolean getEachMethodAsTestCase() {
    return eachMethodAsTestCase == null ? false : eachMethodAsTestCase;
  }

  public void setEachMethodAsTestCase(Boolean eachMethodAsTestCase) {
    this.eachMethodAsTestCase = eachMethodAsTestCase;
  }

  public Configuration setReadFromJenkins(Boolean readFromJenkins) {
    this.readFromJenkins = readFromJenkins;
    return this;
  }

  public String getResultPattern() {
    return resultPattern;
  }

  public Configuration setResultPattern(String resultPattern) {
    this.resultPattern = resultPattern;
    return this;
  }

  public boolean isSubmitToContainer() {
    return submitToContainer;
  }

  public Configuration setSubmitToContainer(boolean submitToContainer) {
    this.submitToContainer = submitToContainer;
    return this;
  }

  public boolean isCreateNewTestRunsEveryBuild() {
    return createNewTestRunsEveryBuild;
  }

  public Configuration setCreateNewTestRunsEveryBuild(boolean createNewTestRunsEveryBuild) {
    this.createNewTestRunsEveryBuild = createNewTestRunsEveryBuild;
    return this;
  }

  public long getContainerId() {
    return containerId;
  }

  public void setContainerId(long containerId) {
    this.containerId = containerId;
  }

  public String getContainerType() {
    return containerType;
  }

  public void setContainerType(String containerType) {
    this.containerType = containerType;
  }

  @Override
  public String toString() {
    return "Configuration{" +
            "id=" + id +
            ", url='" + url + '\'' +
            ", appSecretKey='" + appSecretKey + '\'' +
            ", projectId=" + projectId +
            ", projectName='" + projectName + '\'' +
            ", releaseId=" + releaseId +
            ", releaseName='" + releaseName + '\'' +
            ", environmentId=" + environmentId +
            ", environmentName='" + environmentName + '\'' +
            ", testSuiteId=" + testSuiteId +
            ", moduleId=" + moduleId +
            ", jenkinsServerUrl='" + jenkinsServerUrl + '\'' +
            ", jenkinsProjectName='" + jenkinsProjectName + '\'' +
            ", readFromJenkins=" + readFromJenkins +
            ", resultPattern='" + resultPattern + '\'' +
            ", eachMethodAsTestCase='" + eachMethodAsTestCase + '\'' +
            '}';
  }

  /**
   * @return {@link Setting}
   */
  public Setting toSetting() {
    return new Setting()
            .setId(this.id)
            .setJenkinsServer(this.jenkinsServerUrl)
            .setJenkinsProjectName(this.jenkinsProjectName)
            .setProjectId(this.projectId)
            .setReleaseId(this.releaseId)
            .setModuleId(this.moduleId)
            .setEnvironmentId(this.environmentId)
            .setTestSuiteId(this.testSuiteId);
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Configuration> {
    public String getDisplayName() {
      return "";
    }
  }
}
