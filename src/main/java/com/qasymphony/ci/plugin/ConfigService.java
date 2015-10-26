package com.qasymphony.ci.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
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
   * Client secret key name, application name
   */
  public static final String CLIENT_SECRET_KEY = "qTestJenkins";

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
   * Build headers
   *
   * @param apiKey
   * @param headers
   * @return
   */
  private static Map<String, String> buildHeader(String apiKey, Map<String, String> headers) {
    Map<String, String> map = new HashMap<>();
    map.put("Authorization", apiKey);
    map.put("Content-Type", "application/json");
    if (null != headers && headers.size() > 0) {
      map.putAll(headers);
    }
    return map;
  }

  /**
   * @param qTestUrl
   * @param apiKey
   * @return
   */
  public static Object getProjects(String qTestUrl, String apiKey) {
    String url = String.format("%s/api/v3/projects", qTestUrl);
    try {
      ResponseEntity responseEntity = HttpClientUtils.get(url, buildHeader(apiKey, null));
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
      ResponseEntity responseEntity = HttpClientUtils.get(url, buildHeader(apiKey, null));
      return responseEntity.getBody();
    } catch (ClientRequestException e) {
      LOG.log(Level.WARNING, "Cannot get release from: " + qTestUrl + "," + e.getMessage());
      return null;
    }
  }

  /**
   * @param qTestUrl
   * @param apiKey
   * @param projectId
   * @return
   */
  public static Object getEnvironments(String qTestUrl, String apiKey, Long projectId) {
    //TODO: get environment from qtest by project
    return null;
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
