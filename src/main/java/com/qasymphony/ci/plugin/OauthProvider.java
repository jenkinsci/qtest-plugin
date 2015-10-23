package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.model.Configuration;

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
}
