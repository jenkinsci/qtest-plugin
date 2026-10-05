package com.qasymphony.ci.plugin.utils;

import com.qasymphony.ci.plugin.testsupport.LocalHttpsFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression test for QTEST-39511 (Jenkins SECURITY-3841): HttpClientUtils used to disable TLS
 * certificate and hostname validation unconditionally. These tests stand up a real local HTTPS
 * server with a self-signed certificate (not in any trust store) and assert:
 * - by default, HttpClientUtils.getHttpClient(...) rejects the handshake;
 * - only with the explicit ALLOW_INSECURE_SSL_PROPERTY opt-in does it still connect.
 */
public class HttpClientUtilsTlsTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private LocalHttpsFixture fixture;

  @Before
  public void setUp() throws Exception {
    HttpClientUtils.resetClient();
    System.clearProperty(HttpClientUtils.ALLOW_INSECURE_SSL_PROPERTY);
    fixture = LocalHttpsFixture.startRespondingWith(tempFolder.getRoot(), "ok");
  }

  @After
  public void tearDown() {
    if (fixture != null) {
      fixture.close();
    }
    HttpClientUtils.resetClient();
    System.clearProperty(HttpClientUtils.ALLOW_INSECURE_SSL_PROPERTY);
  }

  @Test
  public void byDefaultUntrustedCertificateIsRejected() throws Exception {
    org.apache.http.client.HttpClient client = HttpClientUtils.getHttpClient(fixture.getBaseUrl() + "/");
    try {
      client.execute(new org.apache.http.client.methods.HttpGet(fixture.getBaseUrl() + "/"));
      fail("Expected the handshake to fail against an untrusted self-signed certificate");
    } catch (javax.net.ssl.SSLException expected) {
      // certificate validation correctly rejected the connection
    } catch (org.apache.http.conn.HttpHostConnectException e) {
      // some JVM/HttpClient combinations surface the SSL failure wrapped differently; only
      // accept it if the root cause is genuinely an SSL trust failure, not a real network issue
      Throwable cause = e.getCause();
      assertTrue("Expected an SSL trust failure, got: " + e,
              cause instanceof javax.net.ssl.SSLException);
    }
  }

  @Test
  public void withEscapeHatchUntrustedCertificateIsAccepted() throws Exception {
    System.setProperty(HttpClientUtils.ALLOW_INSECURE_SSL_PROPERTY, "true");
    HttpClientUtils.resetClient();

    org.apache.http.client.HttpClient client = HttpClientUtils.getHttpClient(fixture.getBaseUrl() + "/");
    org.apache.http.HttpResponse response = client.execute(
            new org.apache.http.client.methods.HttpGet(fixture.getBaseUrl() + "/"));
    assertEquals(200, response.getStatusLine().getStatusCode());
  }
}
