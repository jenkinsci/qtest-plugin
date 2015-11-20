package com.qasymphony.ci.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.action.PushingResultAction;
import com.qasymphony.ci.plugin.exception.SaveSettingException;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author trongle
 * @version 10/22/2015 11:05 PM trongle $
 * @since 1.0
 */
public class ConfigService {
  private static final Logger LOG = Logger.getLogger(ConfigService.class.getName());

  /**
   * Field name in json when get field setting
   */
  private static final String FIELD_ORIGIN_NAME = "original_name";
  /**
   * Origin value of environment field in field setting of testSuite in qTest
   */
  private static final String FIELD_ENVIRONMENT_ORIGIN_NAME = "EnvironmentTestSuite";

  private ConfigService() {

  }

  /**
   * Validate qTest Url
   *
   * @param url
   * @return
   */
  public static Boolean validateQtestUrl(String url) {
    String versionUrl = String.format("%s%s", url, "/version");
    try {
      ResponseEntity entity = HttpClientUtils.get(versionUrl, null);
      if (!StringUtils.isEmpty(entity.getBody())) {
        JsonNode node = JsonUtils.readTree(entity.getBody());
        String name = JsonUtils.getText(node, "name");
        return "test-conductor".equalsIgnoreCase(name) || "${pom.name}".equalsIgnoreCase(name);
      }
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot connect to qTest." + e.getMessage());
    }
    return false;
  }

  /**
   * Validate apiKey is valid in qTest
   *
   * @param url
   * @param apiKey
   * @return
   */
  public static Boolean validateApiKey(String url, String apiKey) {
    return !StringUtils.isEmpty(OauthProvider.getAccessToken(url, apiKey));
  }

  /**
   * Build testSuite link to qTest
   *
   * @param url
   * @param projectId
   * @param testSuiteId
   * @return
   */
  public static String formatTestSuiteLink(String url, Long projectId, Long testSuiteId) {
    return String.format("%s/p/%s/portal/project#tab=testexecution&object=2&id=%s",
      url, projectId, testSuiteId);
  }

  public static Configuration getPluginConfiguration(AbstractProject project) {
    DescribableList<Publisher, Descriptor<Publisher>> publishers = project.getPublishersList();

    for (int i = 0; i < publishers.size(); i++) {
      if (publishers.get(i) instanceof PushingResultAction) {
        return ((PushingResultAction) publishers.get(i)).getConfiguration();
      }
    }
    return null;
  }

  /**
   * @param qTestUrl
   * @param apiKey
   * @return
   */
  public static Object getProjects(String qTestUrl, String apiKey) {
    String url = String.format("%s/api/v3/projects?assigned=true", qTestUrl);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeaders(qTestUrl, apiKey, null));
      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        return null;
      }
      return responseEntity.getBody();
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot get project from: " + qTestUrl + "," + e.getMessage());
      return null;
    }
  }

  /**
   * @param qTestUrl
   * @param accessToken
   * @param projectId
   * @return
   */
  public static Object getReleases(String qTestUrl, String accessToken, Long projectId) {
    String url = String.format("%s/api/v3/projects/%s/releases?includeClosed=true", qTestUrl, projectId);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeaders(accessToken, null));
      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        return null;
      }
      return responseEntity.getBody();
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot get release from: " + qTestUrl + "," + e.getMessage());
      return null;
    }
  }

  /**
   * Get environment values of testSuite
   *
   * @param qTestUrl
   * @param accessToken
   * @param projectId
   * @return
   */
  public static Object getEnvironments(String qTestUrl, String accessToken, Long projectId) {
    String url = String.format("%s/api/v3/projects/%s/settings/test-suites/fields?includeInactive=true", qTestUrl, projectId);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeaders(accessToken, null));
      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        return null;
      }
      JSONArray fields = StringUtils.isEmpty(responseEntity.getBody()) ? null : JSONArray.fromObject(responseEntity.getBody());
      if (null == fields || fields.size() <= 0)
        return null;
      JSONObject envObject = null;
      for (int i = 0; i < fields.size(); i++) {
        JSONObject fieldObject = fields.getJSONObject(i);
        if (null != fieldObject && FIELD_ENVIRONMENT_ORIGIN_NAME.equalsIgnoreCase(fieldObject.getString(FIELD_ORIGIN_NAME))) {
          envObject = fieldObject;
          break;
        }
      }
      return envObject;
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot get environment values from: " + qTestUrl + "," + e.getMessage());
      return null;
    }
  }

  /**
   * Get saved configuration from qTest
   *
   * @param setting
   * @param qTestUrl
   * @param accessToken
   * @return
   */
  public static Object getConfiguration(Setting setting, String qTestUrl, String accessToken) {
    Boolean getById = setting.getId() != null && setting.getId() > 0;

    String urlById = String.format("%s/api/v3/projects/%s/ci/%s", qTestUrl, setting.getProjectId(), setting.getId());
    String urlByProject = String.format("%s/api/v3/projects/%s/ci?server=%s&project=%s&type=jenkins", qTestUrl, setting.getProjectId(),
      setting.getJenkinsServer(), HttpClientUtils.encode(setting.getJenkinsProjectName()));

    try {
      Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
      ResponseEntity responseEntity = HttpClientUtils.get(getById ? urlById : urlByProject, headers);

      if (HttpStatus.SC_OK != responseEntity.getStatusCode() && getById) {
        //in case not found by id, we try to get by project
        responseEntity = HttpClientUtils.get(urlByProject, headers);
      }

      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        LOG.log(Level.WARNING, String.format("Cannot get config from qTest:%s, server:%s, project:%s, error:%s",
          qTestUrl, setting.getJenkinsServer(), setting.getJenkinsProjectName(), responseEntity.getBody()));
        return null;
      }
      LOG.info(String.format("Get config from qTest:%s,%s", qTestUrl, responseEntity.getBody()));
      return responseEntity.getBody();
    } catch (ClientRequestException e) {
      return null;
    }
  }

  /**
   * Validate configuration from save or apply setting
   *
   * @param configuration
   * @param formData
   * @return
   */
  public static Configuration validateConfiguration(Configuration configuration, JSONObject formData) {
    //make id is 0 when name is empty, we get name from selectize field.
    if (StringUtils.isEmpty(formData.getString("environmentName1"))) {
      configuration.setEnvironmentId(0);
      configuration.setEnvironmentName("");
    }
    if (StringUtils.isEmpty(formData.getString("projectName1"))) {
      configuration.setProjectId(0);
      configuration.setProjectName("");
    }
    if (StringUtils.isEmpty(formData.getString("releaseName1"))) {
      configuration.setReleaseId(0);
      configuration.setReleaseName("");
    }
    if (configuration.getProjectId() <= 0 || configuration.getReleaseId() <= 0) {
      configuration.setId(0L);
      configuration.setModuleId(0);
    }
    return configuration;
  }

  /**
   * @param configuration
   * @return
   */
  public static Setting saveConfiguration(Configuration configuration) throws SaveSettingException {
    LOG.info("Save configuration to qTest:" + configuration);
    try {
      //get access token
      String accessToken = OauthProvider.getAccessToken(configuration.getUrl(), configuration.getAppSecretKey());

      //get saved setting from qTest
      Object savedObject = getConfiguration(configuration.toSetting(), configuration.getUrl(), accessToken);
      Setting savedSetting = null == savedObject ? null : JsonUtils.fromJson(savedObject.toString(), Setting.class);

      //prepare for send request to qTest
      Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
      Setting setting = configuration.toSetting();
      ResponseEntity responseEntity = null;

      if (savedSetting != null && savedSetting.getId() > 0) {
        String url = String.format("%s/api/v3/projects/%s/ci/%s", configuration.getUrl(), configuration.getProjectId(), savedSetting.getId());
        responseEntity = HttpClientUtils.put(url, headers, JsonUtils.toJson(setting));
      } else {
        String url = String.format("%s/api/v3/projects/%s/ci", configuration.getUrl(), configuration.getProjectId());
        responseEntity = HttpClientUtils.post(url, headers, JsonUtils.toJson(setting));
      }

      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        LOG.log(Level.WARNING, String.format("Cannot save config to qTest, statusCode:%s, error:%s",
          responseEntity.getStatusCode(), responseEntity.getBody()));
        throw new SaveSettingException("Cannot save setting: " + responseEntity.getBody(), responseEntity.getStatusCode());
      }
      Setting res = JsonUtils.fromJson(responseEntity.getBody(), Setting.class);
      LOG.info("Saved from qTest:" + responseEntity.getBody());
      return res;
    } catch (ClientRequestException e) {
      throw new SaveSettingException("Cannot save setting:" + e.getMessage(), -1);
    }
  }
}
