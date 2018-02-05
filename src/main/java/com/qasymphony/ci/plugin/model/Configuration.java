package com.qasymphony.ci.plugin.model;

import com.qasymphony.ci.plugin.model.qtest.Container;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author anpham
 */
public class Configuration extends PipelineConfiguration implements hudson.model.Describable<PipelineConfiguration> {
  private Long id;

  /**
   * Refresh token
   */

  private String projectName;
  private String releaseName;
  private String environmentName;
  private long testSuiteId;
  private String jenkinsServerUrl;
  private String jenkinsProjectName;
  private String containerSetting;
  private long environmentParentId;
  /**
   * Read from testResult action from jenkins
   */
  public String getContainerSetting() {
    return containerSetting;
  }

  public void setContainerSetting(String containerSetting) {
    this.containerSetting = containerSetting;
  }

  public static Configuration newInstance() {
    return new Configuration(0L, "", "", 0L, "", 0L, "", 0L, "",
            0L, 0L, false, "", false, "{}", false, 0L);
  }

  @DataBoundConstructor
  public Configuration(Long id, String url, String appSecretKey, Long projectId,
                       String projectName, Long releaseId, String releaseName, Long environmentId,
                       String environmentName, Long testSuiteId, Long moduleId, Boolean readFromJenkins, String resultPattern,
                       Boolean submitToContainer, String containerSetting, Boolean overwriteExistingTestSteps, Long environmentParentId) {
    super(url,
            appSecretKey,
            projectId,
            releaseId,
            "release",
            environmentId,
            resultPattern,
            overwriteExistingTestSteps,
            false, // fake data, is set in newInstance
            readFromJenkins,
            false, // fake data, is set in newInstance
            submitToContainer);
    this.projectName = projectName;
    this.releaseName = releaseName;
    this.environmentName = environmentName;
    this.testSuiteId = testSuiteId;
    this.id = id;
    this.containerSetting = containerSetting;
    this.environmentParentId = environmentParentId;
  }

  public Long getId() {
    return id;
  }

  public Configuration setId(Long id) {
    this.id = id;
    return this;
  }


  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public long getReleaseId() {
    return containerId;
  }

  public void setReleaseId(long releaseId) {
    this.containerId = releaseId;
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



  public long getTestSuiteId() {
    return testSuiteId;
  }

  public void setTestSuiteId(long testSuiteId) {
    this.testSuiteId = testSuiteId;
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


  public boolean getReadFromJenkins() {
    //new version: make default read by parse file
    return readFromJenkins == null ? true : readFromJenkins;
  }


  public Boolean getEachMethodAsTestCase() {
    return eachMethodAsTestCase == null ? false : eachMethodAsTestCase;
  }

  public void setEachMethodAsTestCase(Boolean eachMethodAsTestCase) {
    this.eachMethodAsTestCase = eachMethodAsTestCase;
  }

  public Boolean isOverwriteExistingTestSteps() {
    if (null == this.containerSetting)
      overwriteExistingTestSteps = true; // for backward compatible
    return overwriteExistingTestSteps;
  }


  public Boolean isCreateNewTestSuiteEveryBuild() {
    try {
      JSONObject json = JSONObject.fromObject(this.containerSetting);
      JSONObject selectedContainer = json.getJSONObject("selectedContainer");
      if (selectedContainer.has("dailyCreateTestSuite")) {
        return selectedContainer.getBoolean("dailyCreateTestSuite");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return false;
  }

  public long getEnvironmentParentId() {
    return environmentParentId;
  }

  public void setEnvironmentParentId(long environmentParentId) {
    this.environmentParentId = environmentParentId;
  }


  @Override
  public String toString() {
    return "Configuration{" +
      "id=" + id +
      ", url='" + url + '\'' +
      ", appSecretKey='" + appSecretKey + '\'' +
      ", projectId=" + projectId +
      ", projectName='" + projectName + '\'' +
      ", releaseId=" + containerId +
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
    Setting setting = new Setting()
            .setId(this.id)
            .setJenkinsServer(this.jenkinsServerUrl)
            .setJenkinsProjectName(this.jenkinsProjectName)
            .setProjectId(this.projectId)
            .setModuleId(this.moduleId)
            .setEnvironmentId(this.environmentId)
            .setOverwriteExistingTestSteps(this.overwriteExistingTestSteps)
            .setTestSuiteId(this.testSuiteId);

    if (this.submitToContainer) {
      long nodeId = -1;
      String nodeType = "";
      boolean createTestSuiteEveryBuildDate = false;
      try {
        JSONObject json = JSONObject.fromObject(this.containerSetting);
        JSONObject selectedContainer = json.getJSONObject("selectedContainer");
        if (selectedContainer.has("dailyCreateTestSuite")) {
          createTestSuiteEveryBuildDate = selectedContainer.getBoolean("dailyCreateTestSuite");
        }

        JSONArray containerPath = JSONArray.fromObject(json.getString("containerPath"));
        if (0 < containerPath.size()) {
          JSONObject jsonContainer = containerPath.getJSONObject(containerPath.size() - 1);
          if (null != jsonContainer) {
            nodeType = jsonContainer.getString("nodeType");
            nodeId = jsonContainer.getLong("nodeId");
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      Container container = new Container();
      container.setId(nodeId);
      container.setType(nodeType);
      container.setCreateNewTestSuiteEveryBuild(createTestSuiteEveryBuildDate);
      setting.setContainer(container);
    } else {
      setting.setReleaseId(this.containerId);
    }
    return setting;
  }

  public  String getFakeContainerName() {
    try {
      JSONObject json = JSONObject.fromObject(this.containerSetting);
      JSONObject selectedContainer = json.getJSONObject("selectedContainer");
      if (selectedContainer.has("name")) {
        return selectedContainer.getString("name");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  public JSONObject getContainerJSONObject() {
    try {
      JSONObject json = JSONObject.fromObject(this.containerSetting);
      JSONArray containerPath = JSONArray.fromObject(json.getString("containerPath"));
      json.put("containerPath", containerPath);
      return json;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<PipelineConfiguration> {
    public String getDisplayName() {
      return "";
    }
  }
}
