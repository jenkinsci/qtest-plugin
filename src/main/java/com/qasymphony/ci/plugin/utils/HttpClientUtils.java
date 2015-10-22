package com.qasymphony.ci.plugin.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class HttpClientUtils {
  private HttpClientUtils() {
  }

  private static HttpClient CLIENT;

  private static HttpClient getClient() throws ClientRequestException {
    initClient();
    return CLIENT;
  }

  private static synchronized void initClient() throws ClientRequestException {
    if (null == CLIENT) {
      try {
        CLIENT = getHttpClient();
      } catch (Exception e) {
        throw new ClientRequestException(e.getMessage());
      }
    }
  }

  /**
   * @param url
   * @param headers
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity get(String url, Map<String, String> headers) throws ClientRequestException {
    HttpGet request = new HttpGet(url);
    addHeader(request, headers);
    return execute(request);
  }

  /**
   * @param url
   * @param headers
   * @param data
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity post(String url, Map<String, String> headers, String data)
    throws ClientRequestException {
    return post(url, headers, data, ContentType.APPLICATION_JSON);
  }

  /**
   * @param url
   * @param headers
   * @param data
   * @param contentType
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity post(String url, Map<String, String> headers, String data, ContentType contentType)
    throws ClientRequestException {
    HttpPost request = new HttpPost(url);
    addHeader(request, headers);
    if (!StringUtils.isEmpty(data))
      request.setEntity(new StringEntity(data, contentType));
    return execute(request);
  }

  /**
   * @param url
   * @param headers
   * @param data
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity put(String url, Map<String, String> headers, String data) throws ClientRequestException {
    return put(url, headers, data, ContentType.APPLICATION_JSON);
  }

  /**
   * @param url
   * @param headers
   * @param data
   * @param contentType
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity put(String url, Map<String, String> headers, String data, ContentType contentType)
    throws ClientRequestException {
    HttpPut request = new HttpPut(url);
    addHeader(request, headers);
    if (!StringUtils.isEmpty(data))
      request.setEntity(new StringEntity(data, contentType));
    return execute(request);
  }

  /**
   * @param url
   * @param headers
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity delete(String url, Map<String, String> headers) throws ClientRequestException {
    return delete(url, headers, ContentType.APPLICATION_JSON);
  }

  /**
   * @param url
   * @param headers
   * @param contentType
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity delete(String url, Map<String, String> headers, ContentType contentType)
    throws ClientRequestException {
    HttpDelete request = new HttpDelete(url);
    addHeader(request, headers);
    return execute(request);
  }

  private static ResponseEntity doExecute(HttpResponse response) throws ClientRequestException {
    if (null == response) {
      throw new ClientRequestException("response is null.");
    }
    HttpEntity entity = response.getEntity();
    BufferedReader rd = null;
    try {
      rd = new BufferedReader(
        new InputStreamReader(entity.getContent()));
    } catch (IOException e) {
      throw new ClientRequestException(e.getMessage(), e);
    }
    StringBuilder result = new StringBuilder();
    String line = "";
    try {
      while ((line = rd.readLine()) != null) {
        result.append(line);
      }
    } catch (IOException e) {
      throw new ClientRequestException(e.getMessage());
    }
    return new ResponseEntity(result.toString(), response.getStatusLine().getStatusCode());
  }

  /**
   * Execute a request
   *
   * @param request
   * @return
   * @throws ClientRequestException
   */
  public static ResponseEntity execute(HttpUriRequest request) throws ClientRequestException {
    HttpClient client;
    try {
      client = getClient();
    } catch (Exception e) {
      throw new ClientRequestException("Cannot get HttpClient." + e.getMessage(), e);
    }
    HttpResponse response = null;
    ResponseEntity responseEntity;

    try {
      response = client.execute(request);
      responseEntity = doExecute(response);
    } catch (IOException e) {
      throw new ClientRequestException(e.getMessage(), e);
    } finally {
      if (null != response)
        org.apache.http.client.utils.HttpClientUtils.closeQuietly(response);
    }

    return responseEntity;
  }

  private static void addHeader(HttpRequestBase httpRequestBase, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequestBase.addHeader(entry.getKey(), entry.getValue());
      }
    }
  }

  public static HttpClient getHttpClient() throws Exception {
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    SSLConnectionSocketFactory sslSocketFactory = getSslSocketFactory();
    httpClientBuilder.setSSLSocketFactory(sslSocketFactory)
      .setConnectionReuseStrategy(new NoConnectionReuseStrategy());
    return httpClientBuilder.build();
  }

  private static SSLConnectionSocketFactory getSslSocketFactory()
    throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = getSslContext();
    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
      ALLOW_ALL_HOSTNAME_VERIFIER);
    return sslSocketFactory;
  }

  private static SSLContext getSslContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    TrustStrategy trustStrategy = new TrustAllStrategy();
    sslContextBuilder.loadTrustMaterial(keyStore, trustStrategy);
    SSLContext sslContext = sslContextBuilder.build();
    return sslContext;
  }

  /**
   * Trust all certificates.
   */
  public static class TrustAllStrategy implements TrustStrategy {

    @Override
    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      return true;
    }
  }
}