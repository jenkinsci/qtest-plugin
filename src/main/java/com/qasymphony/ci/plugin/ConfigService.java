package com.qasymphony.ci.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

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
   * Client secret key name, application name
   */
  public static final String CLIENT_SECRET_KEY = "qTestJenkins";

  /**
   * Field name in json when get field setting
   */
  private static final String FIELD_ORIGIN_NAME = "original_name";
  /**
   * Origin value of environment field in field setting of testSuite in qTest
   */
  private static final String FIELD_ENVIRONMENT_ORIGIN_NAME = "EnvironmentTestSuite";

  /**
   * Allowed value for field environment
   */
  private static final String FIELD_ENVIRONMENT_ALLOWED_VALUE = "allowed_values";

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
        return "test-conductor".equalsIgnoreCase(name);
      }
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot connect to qTest." + e.getMessage());
    }
    return false;
  }

  /**
   * @param qTestUrl
   * @param apiKey
   * @return
   */
  public static Object getProjects(String qTestUrl, String apiKey) {
    String url = String.format("%s/api/v3/projects", qTestUrl);
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
    String url = String.format("%s/api/v3/projects/%s/releases", qTestUrl, projectId);
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
    //TODO: get environment from qtest by project
    String url = String.format("%s/api/v3/projects/%s/settings/test-suites/fields", qTestUrl, projectId);
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
      return (null != envObject) ? envObject.getJSONArray(FIELD_ENVIRONMENT_ALLOWED_VALUE) : null;
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
  public static Object getConfiguration(String qTestUrl, String apiKey) {
    //TODO: get configuration from qTest API
    return null;
  }

  /**
   * @param configuration
   * @return
   */
  public static Object saveConfiguration(Configuration configuration) {
    LOG.info("Save configuration to qTest:" + configuration);
    return configuration;
  }
}
