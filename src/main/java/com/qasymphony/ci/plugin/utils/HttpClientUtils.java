package com.qasymphony.ci.plugin.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class HttpClientUtils {

  public HttpRequestBase createRequestBase(HttpRequestMethod httpRequestMethod) throws IOException {
    return null;
  }

  private HttpEntity makeEntity(List<NameValuePair> params) throws
    UnsupportedEncodingException {
    return new UrlEncodedFormEntity(params);
  }

  public HttpResponse execute(DefaultHttpClient client, HttpContext context, HttpRequestBase method,
    PrintStream logger, Integer timeout) throws IOException, InterruptedException {
    doSecurity(client, method.getURI());

    logger.println("Sending request to url: " + method.getURI());

    if (timeout != null) {
      Integer timeoutNumber = timeout * 1000;
      client.getParams().setParameter("http.socket.timeout", timeoutNumber);
      client.getParams().setParameter("http.connection.timeout", timeoutNumber);
      client.getParams().setParameter("http.connection-manager.timeout", timeoutNumber);
      client.getParams().setParameter("http.protocol.head-body-timeout", timeoutNumber);
    }

    final HttpResponse httpResponse = client.execute(method, context);
    logger.println("Response Code: " + httpResponse.getStatusLine());

    return httpResponse;
  }

  private void doSecurity(DefaultHttpClient base, URI uri) throws IOException {
    if (!uri.getScheme().equals("https")) {
      return;
    }

    try {
      final SSLSocketFactory ssf = new SSLSocketFactory(new TrustStrategy() {
        public boolean isTrusted(X509Certificate[] chain,
          String authType) throws CertificateException {
          return true;
        }
      }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

      final SchemeRegistry schemeRegistry = base.getConnectionManager().getSchemeRegistry();
      final int port = uri.getPort() < 0 ? 443 : uri.getPort();
      schemeRegistry.register(new Scheme(uri.getScheme(), port, ssf));
    } catch (Exception ex) {
      throw new IOException("Error unknown", ex);
    }
  }
}