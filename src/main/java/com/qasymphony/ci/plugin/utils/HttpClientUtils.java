package com.qasymphony.ci.plugin.utils;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.kohsuke.stapler.StaplerRequest;

import java.net.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.List;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
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
  public static Integer RETRY_MAX_COUNT = 5;
  public static Boolean RETRY_REQUEST_SEND_RETRY_ENABLED = false;
  /**
   * Opt-in escape hatch for on-prem qTest servers with a self-signed certificate.
   * Off by default: TLS certificate and hostname validation are always enforced unless an
   * operator explicitly sets this JVM system property to "true" on the Jenkins controller.
   * See QTEST-39511 / Jenkins SECURITY-3841.
   */
  public static final String ALLOW_INSECURE_SSL_PROPERTY = "qtest.plugin.allowInsecureSsl";
  private static final Integer DEFAULT_SOCKET_TIMEOUT = 60;//seconds
  private static HttpClient CLIENT;
  private static final Logger LOG = Logger.getLogger(HttpClientUtils.class.getName());

  private HttpClientUtils() {
  }

  private static HttpClient getClient(String hostUrl) throws ClientRequestException {
    initClient(hostUrl);
    return CLIENT;
  }

  private static synchronized void initClient(String hostUrl) throws ClientRequestException {
    if (null == CLIENT && null != hostUrl) {
      try {
        CLIENT = getHttpClient(hostUrl);
      } catch (Exception e) {
        throw new ClientRequestException(e.getMessage());
      }
    }
  }

  /**
   * Test-only: drop the cached client so the next call rebuilds it (e.g. against a different
   * test server or SSL configuration). Not used by production code paths. Public so tests in
   * other packages can reset the cache between runs, since CLIENT is otherwise built once per
   * JVM and Surefire typically runs the whole plugin's test suite in a single fork.
   */
  public static synchronized void resetClient() {
    CLIENT = null;
  }

  private static boolean isInsecureSslAllowed() {
    return Boolean.getBoolean(ALLOW_INSECURE_SSL_PROPERTY);
  }

  /**
   * Encode url
   *
   * @param url url
   * @return encoded string
   */
  public static String encode(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (Exception e) {
      return url;
    }
  }

  /**
   * @param request request
   * @return server url
   */
  public static String getServerUrl(StaplerRequest request) {
    return getServerUrl(request.getServerPort(), request.getScheme(), request.getServerName(), request.getContextPath());
  }

  public static String getServerUrl(int serverPort, String scheme, String serverName, String contextPath) {
    Boolean isDefaultPort = serverPort == 443 || serverPort == 80;
    return String.format("%s://%s%s%s", scheme, serverName, isDefaultPort ? "" : ":" + serverPort, contextPath);
  }

  /**
   * get port from url
   *
   * @param url url
   * @return port
   */
  public static int getPort(String url) {
    URL uri = null;
    try {
      uri = new URL(url);
    } catch (Exception e) {
    }
    int port = 0;
    if (uri != null) {
      port = (uri.getPort() > 0 ? uri.getPort() : ("http".equalsIgnoreCase(uri.getProtocol()) ? 80 : 443));
    }
    return port;
  }

  /**
   * get mac address and port
   *
   * @return mac address
   * @throws Exception Exception
   */
  public static String getMacAddress() throws Exception {
    NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

    //if cannot get by localhost, we try to get first NetworkInterface
    if (null == network) {
      network = NetworkInterface.getByIndex(0);
    }

    byte[] mac;
    try {
      mac = network.getHardwareAddress();
    } catch (Exception e) {
      mac = new byte[0];
    }

    if (mac != null && mac.length <= 0) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < mac.length; i++) {
      sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
    }
    return sb.toString();
  }

  /**
   * @param url     url
   * @param headers headers
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
   */
  public static ResponseEntity get(String url, Map<String, String> headers) throws ClientRequestException {
    HttpGet request = new HttpGet(url);
    addHeader(request, headers);
    return execute(request);
  }

  /**
   * @param url     url
   * @param headers headers
   * @param data    data
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
   */
  public static ResponseEntity post(String url, Map<String, String> headers, String data)
          throws ClientRequestException {
    return post(url, headers, data, ContentType.APPLICATION_JSON);
  }

  /**
   * @param url         url
   * @param headers     headers
   * @param data        data
   * @param contentType contentType
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
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
   * @param url     url
   * @param headers headers
   * @param data    data
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
   */
  public static ResponseEntity put(String url, Map<String, String> headers, String data) throws ClientRequestException {
    return put(url, headers, data, ContentType.APPLICATION_JSON);
  }

  /**
   * @param url         url
   * @param headers     headers
   * @param data        data
   * @param contentType contentType
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
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
   * @param url     url
   * @param headers headers
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
   */
  public static ResponseEntity delete(String url, Map<String, String> headers) throws ClientRequestException {
    return delete(url, headers, ContentType.APPLICATION_JSON);
  }

  /**
   * @param url         url
   * @param headers     headers
   * @param contentType contentType
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
   */
  public static ResponseEntity delete(String url, Map<String, String> headers, ContentType contentType)
          throws ClientRequestException {
    HttpDelete request = new HttpDelete(url);
    addHeader(request, headers);
    return execute(request);
  }

  private static ResponseEntity doExecute(HttpResponse response) throws Exception {
    if (null == response) {
      throw new Exception("response is null.");
    }
    HttpEntity entity = response.getEntity();
    BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));

    StringBuilder result = new StringBuilder();
    String line = "";
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    return new ResponseEntity(result.toString(), response.getStatusLine().getStatusCode());
  }

  /**
   * Execute a request
   *
   * @param request request
   * @return {@link ResponseEntity}
   * @throws ClientRequestException ClientRequestException
   */
  public static ResponseEntity execute(HttpUriRequest request) throws ClientRequestException {
    HttpClient client;
    try {
      client = getClient(request.getURI().getHost());
    } catch (Exception e) {
      throw new ClientRequestException("Cannot get HttpClient." + e.getMessage(), e);
    }
    HttpResponse response = null;
    ResponseEntity responseEntity;

    try {
      response = client.execute(request);
      responseEntity = doExecute(response);
    } catch (Exception e) {
      throw new ClientRequestException(e.getMessage(), e);
    } finally {
      if (null != response)
        org.apache.http.client.utils.HttpClientUtils.closeQuietly(response);
    }

    return responseEntity;
  }

  /**
   * Strips CR/LF from a header name or value. Apache HttpClient 4.x does not itself reject
   * embedded CRLF sequences, so header values built from config-provided strings (server URL,
   * project name, etc.) are a CRLF/header-injection sink (CWE-113) otherwise.
   */
  static String stripCrlf(String value) {
    return value == null ? null : value.replaceAll("[\\r\\n]", "");
  }

  static void addHeader(HttpRequestBase httpRequestBase, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequestBase.addHeader(stripCrlf(entry.getKey()), stripCrlf(entry.getValue()));
      }
    }
  }

  private static Boolean isUrlMatchWithNoProxyHost(String hostUrl, List<Pattern> proxyHostPatterns) {
    if (hostUrl != null && proxyHostPatterns != null && !proxyHostPatterns.isEmpty()) {
      Iterator i$ = proxyHostPatterns.iterator();

      while(i$.hasNext()) {
        Pattern p = (Pattern)i$.next();
        if (p.matcher(hostUrl).matches()) {
          return true;
        }
      }
    }
    return false;
  }

  private static void setHttpProxy(HttpClientBuilder httpClientBuilder, String hostUrl) {
    Jenkins jenkins = Jenkins.getInstanceOrNull();
    if (jenkins == null) {
      return;
    }
    ProxyConfiguration proxyConfig = jenkins.proxy;
    LOG.log(Level.INFO, "-- Proxy info: " +  ReflectionToStringBuilder.toString(proxyConfig));
    if (proxyConfig != null) {
      List<Pattern> proxyHostPatterns = proxyConfig.getNoProxyHostPatterns();
      LOG.log(Level.INFO, "-- No proxy host info: " + Arrays.toString(proxyHostPatterns.toArray()));
      if (isUrlMatchWithNoProxyHost(hostUrl, proxyHostPatterns)) {
        LOG.log(Level.INFO, "-- No proxy host has url: " + hostUrl);
        return;
      }
      HttpHost proxy = new HttpHost(proxyConfig.name, proxyConfig.port);
      String username = proxyConfig.getUserName();
      String password = proxyConfig.getPassword();

      Credentials credentials;
      if (username != null && StringUtils.isNotEmpty(username) == true) {
        credentials = new UsernamePasswordCredentials(username, password);
      } else  {
        credentials = new UsernamePasswordCredentials("", "");
      }
      AuthScope authScope = new AuthScope(proxyConfig.name, proxyConfig.port);
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(authScope, credentials);
      httpClientBuilder.useSystemProperties();
      httpClientBuilder.setProxy(proxy).setDefaultCredentialsProvider(credsProvider).build();
    }
  }

  public static HttpClient getHttpClient(String hostUrl) throws Exception {
    int timeout;
    try {
      timeout = Integer.parseInt(System.getenv("SOCKET_TIMEOUT"));
    } catch (Exception e) {
      timeout = DEFAULT_SOCKET_TIMEOUT;
    }

    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

    setHttpProxy(httpClientBuilder, hostUrl);

    SSLConnectionSocketFactory sslSocketFactory = getSslSocketFactory();
    httpClientBuilder.setSSLSocketFactory(sslSocketFactory)
            .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
            .addInterceptorFirst(new UserAgentHeaderInterceptor());

    timeout = timeout * 1000;

    httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom()
            .setSocketTimeout(timeout)
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build());
    httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(RETRY_MAX_COUNT, RETRY_REQUEST_SEND_RETRY_ENABLED) {
      @Override
      public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        if (executionCount > this.getRetryCount())
          return false;
        if (exception instanceof HttpHostConnectException)
          return true;
        return super.retryRequest(exception, executionCount, context);
      }
    });
    return httpClientBuilder.build();
  }

  private static SSLConnectionSocketFactory getSslSocketFactory()
          throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    if (isInsecureSslAllowed()) {
      LOG.log(Level.WARNING, "-- " + ALLOW_INSECURE_SSL_PROPERTY + " is set: TLS certificate and "
              + "hostname validation are DISABLED for all qTest connections. Do not use this in "
              + "production; it exists only for on-prem servers with a self-signed certificate "
              + "that cannot be imported into the Jenkins controller's truststore.");
      return new SSLConnectionSocketFactory(getInsecureSslContext(), ALLOW_ALL_HOSTNAME_VERIFIER);
    }
    return new SSLConnectionSocketFactory(getDefaultSslContext(),
            SSLConnectionSocketFactory.getDefaultHostnameVerifier());
  }

  /**
   * The JVM's default SSLContext: validates the server certificate chain against the
   * controller's own truststore (cacerts, plus anything imported via keytool) and does not
   * override hostname verification. This is the only path used unless
   * {@link #ALLOW_INSECURE_SSL_PROPERTY} is explicitly set.
   */
  private static SSLContext getDefaultSslContext() throws NoSuchAlgorithmException {
    return SSLContext.getDefault();
  }

  private static SSLContext getInsecureSslContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    org.apache.http.ssl.SSLContextBuilder sslContextBuilder = new org.apache.http.ssl.SSLContextBuilder();
    TrustStrategy trustStrategy = new TrustAllStrategy();
    sslContextBuilder.loadTrustMaterial(null, trustStrategy);
    return sslContextBuilder.build();
  }

  /**
   * Trust all certificates. Only reachable when an operator has explicitly opted in via
   * {@link #ALLOW_INSECURE_SSL_PROPERTY} -- see QTEST-39511.
   */
  public static class TrustAllStrategy implements TrustStrategy  {

    @Override
    public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      return true;
    }
  }
}