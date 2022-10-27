package com.qasymphony.ci.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.exception.OAuthException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author trongle
 * @version 10/23/2015 5:22 PM trongle $
 * @since 1.0
 */
public class OauthProvider {
  private static final Logger LOG = Logger.getLogger(OauthProvider.class.getName());
  public static String HEADER_KEY = "Basic amVua2luczpkZEtzVjA4NmNRbW8wWjZNUzBCaU4wekpidVdLbk5oNA==";

  private OauthProvider() {

  }

  /**
   * Get access token from apiKey
   *
   * @param url    url
   * @param apiKey apiKey
   * @return access token
   * @throws OAuthException OAuthException
   */
  public static String getAccessToken(String url, String apiKey) throws OAuthException {
    return getAccessToken(url, apiKey, HEADER_KEY);
  }

  public static String getAccessToken(String url, String apiKey, String secretKey) throws OAuthException {
    StringBuilder sb = new StringBuilder()
      .append(url)
      .append("/oauth/token?grant_type=refresh_token")
      .append("&refresh_token=").append(HttpClientUtils.encode(apiKey));
    Map<String, String> headers = new HashMap<>();
    headers.put(Constants.HEADER_AUTH, secretKey);
    try {
      ResponseEntity entity = HttpClientUtils.post(sb.toString(), headers, null);
      if (HttpURLConnection.HTTP_OK != entity.getStatusCode()) {
        throw new OAuthException(entity.getBody(), entity.getStatusCode());
      }
      JsonNode node = JsonUtils.readTree(entity.getBody());
      if (null == node) {
        throw new OAuthException("Cannot get access token from: " + entity.getBody(), entity.getStatusCode());
      }
      return JsonUtils.getText(node, "access_token");
    } catch (Exception e) {
      throw new OAuthException(e.getMessage(), e);
    }
  }

  /**
   * Build header with get access token from refresh token
   *
   * @param url     url
   * @param apiKey  apiKey
   * @param headers headers
   * @return headers
   */
  public static Map<String, String> buildHeaders(String url, String apiKey, Map<String, String> headers) {
    String accessToken = null;
    try {
      accessToken = getAccessToken(url, apiKey);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Error while build header:" + e.getMessage());
    }
    return buildHeaders(accessToken, headers);
  }

  /**
   * Build headers with access token
   *
   * @param accessToken accessToken
   * @param headers     headers
   * @return headers
   */
  public static Map<String, String> buildHeaders(String accessToken, Map<String, String> headers) {
    Map<String, String> map = new HashMap<>();
    //appSecretKey is refresh token, so we packed with Bearer in header when build headers to qTest
    map.put(Constants.HEADER_AUTH, "Bearer " + accessToken);
    map.put(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
    if (null != headers && headers.size() > 0) {
      map.putAll(headers);
    }
    return map;
  }
}
