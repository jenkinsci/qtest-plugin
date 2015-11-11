package com.qasymphony.ci.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
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
    return true;
  }

  /**
   * @param qTestUrl
   * @param apiKey
   * @return
   */
  public static Object getProjects(String qTestUrl, String apiKey) {
    String url = String.format("%s/api/v3/projects?assigned=true", qTestUrl);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeader(apiKey, null));
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
   * @param apiKey
   * @param projectId
   * @return
   */
  public static Object getReleases(String qTestUrl, String apiKey, Long projectId) {
    String url = String.format("%s/api/v3/projects/%s/releases?includeClosed=true", qTestUrl, projectId);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeader(apiKey, null));
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
   * @param apiKey
   * @param projectId
   * @return
   */
  public static Object getEnvironments(String qTestUrl, String apiKey, Long projectId) {
    String url = String.format("%s/api/v3/projects/%s/settings/test-suites/fields?includeInactive=true", qTestUrl, projectId);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, OauthProvider.buildHeader(apiKey, null));
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
   * @param qTestUrl
   * @param apiKey
   * @return
   */
  public static Object getConfiguration(String qTestUrl, String apiKey, String serverName, String projectName, Long projectId) {
    //TODO: get configuration from qTest API
    String url = String.format("%s/api/v3/projects/%s/ci?server=%s&project=%s&type=jenkins",
      qTestUrl, projectId, serverName, projectName);
    try {
      Map<String, String> headers = OauthProvider.buildHeader(apiKey, null);
      ResponseEntity responseEntity = HttpClientUtils.get(url, headers);
      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        LOG.log(Level.WARNING, String.format("Cannot get config from qTest:%s, server:%s, project:%s, error:%s",
          qTestUrl, serverName, projectName, responseEntity.getBody()));
        return null;
      }
      LOG.info(String.format("Get config from qTest:%s,%s", qTestUrl, responseEntity.getBody()));
      return responseEntity.getBody();
    } catch (ClientRequestException e) {
      return null;
    }
  }

  /**
   * @param configuration
   * @return
   */
  public static Setting saveConfiguration(Configuration configuration) {
    LOG.info("Save configuration to qTest:" + configuration);
    String url = String.format("%s/api/v3/projects/%s/ci", configuration.getUrl(), configuration.getProjectId());
    try {
      Map<String, String> headers = OauthProvider.buildHeader(configuration.getAppSecretKey(), null);
      Setting setting = configuration.toSetting();
      ResponseEntity responseEntity = HttpClientUtils.put(url, headers, JsonUtils.toJson(setting));
      if (HttpStatus.SC_OK != responseEntity.getStatusCode()) {
        LOG.log(Level.WARNING, String.format("Cannot save config to qTest, statusCode:%s, error:%s",
          responseEntity.getStatusCode(), responseEntity.getBody()));
        return null;
      }
      Setting res = JsonUtils.fromJson(responseEntity.getBody(), Setting.class);
      LOG.info("Saved from qTest:" + responseEntity.getBody());
      return res;
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot save configuration to qTest: " + configuration.getUrl() + "," + e.getMessage());
      return null;
    }
  }
}
