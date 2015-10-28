package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.model.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author trongle
 * @version 10/23/2015 5:22 PM trongle $
 * @since 1.0
 */
public class OauthProvider {
  private OauthProvider() {

  }

  /**
   * Get access token from jenkins project configured
   *
   * @param configuration
   * @return
   */
  public static String getAccessToken(Configuration configuration) {
    if (null == configuration)
      return null;
    return getAccessToken(configuration.getUrl(), configuration.getAppSecretKey(), ConfigService.CLIENT_SECRET_KEY);
  }

  /**
   * Get access token with client key and app key
   *
   * @param url
   * @param appKey
   * @param clientKey
   * @return
   */
  public static String getAccessToken(String url, String appKey, String clientKey) {
    return null;
  }

  /**
   * Build headers
   *
   * @param apiKey
   * @param headers
   * @return
   */
  public static Map<String, String> buildHeader(String apiKey, Map<String, String> headers) {
    Map<String, String> map = new HashMap<>();
    //TODO: integration with oauth in qTest when oauth ready to get access token
    map.put("Authorization", apiKey);
    map.put("Content-Type", "application/json");
    if (null != headers && headers.size() > 0) {
      map.putAll(headers);
    }
    return map;
  }
}
