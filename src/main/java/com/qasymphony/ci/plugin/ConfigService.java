package com.qasymphony.ci.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.action.PushingResultAction;
import com.qasymphony.ci.plugin.exception.OAuthException;
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
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
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
   * @param url url
   * @return true if url is valid
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
   * @param url    url
   * @param apiKey apiKey
   * @return true if valid api key
   */
  public static Boolean validateApiKey(String url, String apiKey) {
    try {
      return !StringUtils.isEmpty(OauthProvider.getAccessToken(url, apiKey));
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Error while validateApiKey:" + e.getMessage());
      return false;
    }
  }

  /**
   * Build testSuite link to qTest
   *
   * @param url         url
   * @param projectId   projectId
   * @param testSuiteId testSuiteId
   * @return url to testSuite
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
   * @param qTestUrl qTestUrl
   * @param apiKey   apiKey
   * @return a list of projects
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
      LOG.log(Level.WARNING, "Cannot get projects from: " + qTestUrl + "," + e.getMessage());
      return null;
    }
  }

  public static Object getProject(String qTestUrl, String accessToken, Long projectId) {
    String url = String.format("%s/api/v3/projects/%s", qTestUrl, projectId);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeaders(accessToken, null));
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
   * @param qTestUrl    qTestUrl
   * @param accessToken accessToken
   * @param projectId   projectId
   * @return a list of release
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
   * @param qTestUrl    qTestUrl
   * @param accessToken accessToken
   * @param projectId   projectId
   * @return a list of environment field value
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
   * @param setting     setting
   * @param qTestUrl    qTestUrl
   * @param accessToken accessToken
   * @return ci config
   */
  public static Object getConfiguration(Setting setting, String qTestUrl, String accessToken) {
    String urlByProject = String.format("%s/api/v3/projects/%s/ci?server=%s&project=%s&type=jenkins&ciid=%s", qTestUrl, setting.getProjectId(),
      setting.getJenkinsServer(), HttpClientUtils.encode(setting.getJenkinsProjectName()), HttpClientUtils.encode(setting.getServerId()));
    Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
    try {
      //get by qTestProjectId, ci project name, ci server name, type, ciid
      ResponseEntity responseEntity = HttpClientUtils.get(urlByProject, headers);

      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        //in case not found by project, we try to get by id
        String urlById = String.format("%s/api/v3/projects/%s/ci/%s", qTestUrl, setting.getProjectId(), setting.getId());
        responseEntity = HttpClientUtils.get(urlById, headers);
      }

      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        LOG.log(Level.WARNING, String.format("Cannot get config from qTest: %s, %s, error: %s", qTestUrl, setting, responseEntity.getBody()));
        return null;
      }
      Setting res = JsonUtils.fromJson(responseEntity.getBody(), Setting.class);
      LOG.info(String.format("Get config from qTest: %s, response: %s, setting: %s", qTestUrl, responseEntity.getBody(), res));
      if (null != res && Constants.CI_TYPE.equalsIgnoreCase(res.getCiType())) {
        if (StringUtils.isEmpty(res.getServerId()) || setting.getServerId().equalsIgnoreCase(res.getServerId())) {
          return responseEntity.getBody();
        }
      }
      return null;
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Error while get ci setting:" + e.getMessage());
      return null;
    }
  }

  /**
   * Validate configuration from save or apply setting
   *
   * @param configuration configuration
   * @param formData      formData
   * @return {@link Configuration}
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
   * @param configuration configuration
   * @return {@link Setting}
   * @throws Exception Exception
   */
  public static Setting saveConfiguration(Configuration configuration)
    throws Exception {
    LOG.info("Save configuration to qTest:" + configuration);
    String accessToken = OauthProvider.getAccessToken(configuration.getUrl(), configuration.getAppSecretKey());
    try {
      //get saved setting from qTest
      Setting setting = configuration.toSetting();
      setting.setServerId(getServerId(configuration.getJenkinsServerUrl()));
      Object savedObject = getConfiguration(setting, configuration.getUrl(), accessToken);
      Setting savedSetting = null == savedObject ? null : JsonUtils.fromJson(savedObject.toString(), Setting.class);

      //prepare for send request to qTest
      Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
      setting.setCiType(Constants.CI_TYPE);
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
        throw new SaveSettingException(ConfigService.getErrorMessage(responseEntity.getBody()), responseEntity.getStatusCode());
      }
      Setting res = JsonUtils.fromJson(responseEntity.getBody(), Setting.class);
      LOG.info("Saved from qTest:" + responseEntity.getBody());
      return res;
    } catch (Exception e) {
      throw new SaveSettingException(e.getMessage(), -1);
    }
  }

  /**
   * Get plugin version
   *
   * @return version
   */
  public static String getBuildVersion() {
    Package pkg = ConfigService.class.getPackage();
    return StringUtils.isEmpty(pkg.getImplementationVersion()) ?
      pkg.getSpecificationVersion() : pkg.getImplementationVersion();
  }

  /**
   * Parse error message from {@link ResponseEntity}
   *
   * @param body body
   * @return error message
   */
  public static String getErrorMessage(String body) {
    com.qasymphony.ci.plugin.model.Error error = null;
    try {
      error = JsonUtils.parseJson(body, com.qasymphony.ci.plugin.model.Error.class);
    } catch (IOException e) {
      error = null;
    }
    return null == error ? body : error.getMessage();
  }

  /**
   * Get jenkins instance id, when cannot get server id, we try to get mac address and port.
   * 1. get mac address:port
   * 2. if failed, we get server id
   * 3. if failed get by UUID
   *
   * @param jenkinsUrl jenkinsUrl
   * @return server id
   */
  public static String getServerId(String jenkinsUrl) {
    String hmac = null;

    try {
      String macAddress = HttpClientUtils.getMacAddress();
      if (!StringUtils.isEmpty(macAddress)) {
        hmac = String.format("%s:%s", macAddress, HttpClientUtils.getPort(jenkinsUrl));
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Cannot get mac address" + e.getMessage());
    }

    if (!StringUtils.isEmpty(hmac)) {
      return hmac;
    }
    try {
      hmac = Jenkins.getInstance().getLegacyInstanceId();
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Cannot get server id:" + e.getMessage());
    }
    return StringUtils.isEmpty(hmac) ? Constants.JENKINS_SERVER_ID_DEFAULT : hmac;
  }

  public static Object getTestCycleChildren(String qTestUrl, String accessToken, Long projectId, Long parentId, String parentType) {
    String url = String.format("%s/api/v3/projects/%s/test-cycles?expand=descendants", qTestUrl, projectId);
    if (null != parentId) {
        url += "&parentId=" + parentId;
    }
    if (null != parentType) {
        url += "&parentType=" + parentType;
    }
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeaders(accessToken, null));
      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        return null;
      }
      return responseEntity.getBody();
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot get test-cycles from: " + qTestUrl + "," + e.getMessage());
      return null;
    }
  }

    public static Object getTestSuiteChildren(String qTestUrl, String accessToken, Long projectId, Long parentId, String parentType) {
        String url = String.format("%s/api/v3/projects/%s/test-suites?", qTestUrl, projectId);
        if (null != parentId) {
            url += "parentId=" + parentId;
            if (null != parentType) {
                url += "&parentType=" + parentType;
            }
        }
        try {
            ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeaders(accessToken, null));
            if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
                return null;
            }
            return responseEntity.getBody();
        } catch (ClientRequestException e) {
            LOG.log(Level.WARNING, "Cannot get test-suites from: " + qTestUrl + "," + e.getMessage());
            return null;
        }
    }

}
