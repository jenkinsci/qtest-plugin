package com.qasymphony.ci.plugin.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
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

  private static HttpClient getClient() throws Exception {
    initClient();
    return CLIENT;
  }

  private static synchronized void initClient() throws Exception {
    if (null == CLIENT) {
      CLIENT = getHttpClient();
    }
  }

  public static HttpResponse get(String url, Map<String, String> headers) throws Exception {
    HttpGet request = new HttpGet(url);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return getClient().execute(request);
  }

  public static HttpResponse post(String url, Map<String, String> headers) throws Exception {
    HttpPost request = new HttpPost(url);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return getClient().execute(request);
  }

  public static HttpResponse put(String url, Map<String, String> headers) throws Exception {
    HttpPut request = new HttpPut(url);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return getClient().execute(request);
  }

  public static HttpResponse delete(String url, Map<String, String> headers) throws Exception {
    HttpDelete request = new HttpDelete(url);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.addHeader(entry.getKey(), entry.getValue());
      }
    }
    return getClient().execute(request);
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

  public static SSLContext getSslContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
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