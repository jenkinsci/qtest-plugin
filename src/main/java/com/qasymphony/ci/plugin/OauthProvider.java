package com.qasymphony.ci.plugin;

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
   * Build headers
   *
   * @param apiKey
   * @param headers
   * @return
   */
  public static Map<String, String> buildHeader(String apiKey, Map<String, String> headers) {
    Map<String, String> map = new HashMap<>();
    //appSecretKey is refresh token, so we packed with Bearer in header when build headers to qTest
    map.put("Authorization", "Bearer " + apiKey);
    map.put("Content-Type", "application/json");
    if (null != headers && headers.size() > 0) {
      map.putAll(headers);
    }
    return map;
  }
}
