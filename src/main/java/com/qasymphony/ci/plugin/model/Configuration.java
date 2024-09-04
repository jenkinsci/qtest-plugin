package com.qasymphony.ci.plugin.model;

import com.qasymphony.ci.plugin.model.qtest.Container;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author anpham
 */
public class Configuration extends AbstractDescribableImpl<Configuration> {
  private ToscaIntegration toscaIntegration;
  private Long id;
  private String url;
  /**
   * Refresh token
   */
  private String appSecretKey;
  private String secretKey;
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
  private String containerSetting;
  private boolean overwriteExistingTestSteps;


  private long environmentParentId;
  /**
   * Read from testResult action from jenkins
   */
  private Boolean readFromJenkins;
  private String resultPattern;
  private Boolean eachMethodAsTestCase;

  public String getContainerSetting() {
    return containerSetting;
  }

  public void setContainerSetting(String containerSetting) {
    this.containerSetting = containerSetting;
  }



  public static Configuration newInstance() {
    return new Configuration(0L, "", "", "",0, "", 0L, "", 0, "",
            0, 0, false, "", false, "{}", false, 0);
  }

  @DataBoundConstructor
  public Configuration(Long id, String url, String appSecretKey, String secretKey, long projectId,
                       String projectName, long releaseId, String releaseName, long environmentId,
                       String environmentName, long testSuiteId, long moduleId, Boolean readFromJenkins, String resultPattern,
                       Boolean submitToContainer, String containerSetting, Boolean overwriteExistingTestSteps, long environmentParentId) {
    this.url = url;
    this.appSecretKey = appSecretKey;
    this.secretKey = secretKey;
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
    this.containerSetting = containerSetting;
    this.overwriteExistingTestSteps = overwriteExistingTestSteps;
    this.environmentParentId = environmentParentId;
  }

  public Long getId() {
    return id;
  }

  public Configuration setId(Long id) {
    this.id = id;
    return this;
  }

  public ToscaIntegration getToscaIntegration() {
    return toscaIntegration;
  }

  public void setToscaIntegration(ToscaIntegration toscaIntegration) {
    this.toscaIntegration = toscaIntegration;
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

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
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

  public boolean isOverwriteExistingTestSteps() {
    if (null == this.containerSetting)
      overwriteExistingTestSteps = true; // for backward compatible
    return overwriteExistingTestSteps;
  }

  public void setOverwriteExistingTestSteps(boolean overwriteExistingTestSteps) {
    this.overwriteExistingTestSteps = overwriteExistingTestSteps;
  }

  public boolean isCreateNewTestSuiteEveryBuild() {
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
   * @param saveOldSetting create Setting compatible with old version of qTest
   * @return {@link Setting}
   */
  public Setting toSetting(Boolean saveOldSetting) {

    Setting setting = new Setting()
            .setId(this.id)
            .setJenkinsServer(this.jenkinsServerUrl)
            .setJenkinsProjectName(this.jenkinsProjectName)
            .setProjectId(this.projectId)
            .setModuleId(this.moduleId)
            .setEnvironmentId(this.environmentId)
            .setTestSuiteId(this.testSuiteId);

    if (saveOldSetting == true) { // Save old setting for release option if qTest version < 8.9.4
      setting.setReleaseId(this.releaseId);
      return setting;
    }

    setting.setOverwriteExistingTestSteps(this.overwriteExistingTestSteps);

    if (this.submitToContainer) {
      setting.setContainer(this.getContainerInfo());
    } else {
      setting.setReleaseId(this.releaseId);
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

  private Container getContainerInfo() {
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
      return null;
    }

    Container container = new Container();
    container.setId(nodeId);
    container.setType(nodeType);
    container.setCreateNewTestSuiteEveryBuild(createTestSuiteEveryBuildDate);
    return container;
  }
  public JunitSubmitterRequest createJunitSubmitRequest() {
    Long containerID = this.releaseId;
    String containerType = "release";
    Container container = null;
    if (submitToContainer) {
      container = this.getContainerInfo();
      if (null == container){
        return null;
      }
      containerID = container.getId();
      containerType = container.getType();
    }
    JunitSubmitterRequest request = new JunitSubmitterRequest();
    request.setqTestURL(this.url)
            .setApiKey(this.appSecretKey, this.secretKey)
            .setConfigurationID(this.id)
            .setSubmitToExistingContainer(this.submitToContainer)
            .setContainerID(containerID)
            .setContainerType(containerType)
            .setCreateNewTestRunsEveryBuildDate(null != container ? container.getCreateNewTestSuiteEveryBuild() : null)
            .setEnvironmentID(this.environmentId)
            .setEnvironmentParentID(this.environmentParentId)
            .setJenkinsProjectName(this.jenkinsProjectName)
            .setModuleID(this.moduleId)
            .setJenkinsServerURL(this.jenkinsServerUrl)
            .setProjectID(this.projectId);
    return request;

  }

  @Extension
  public static class DescriptorImpl extends Descriptor<Configuration> {
    public String getDisplayName() {
      return "";
    }
  }
}
