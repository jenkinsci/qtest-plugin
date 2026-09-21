package com.qasymphony.ci.plugin.testsupport;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * A local HTTPS server backed by a freshly-generated self-signed certificate (via keytool, no
 * extra test dependency), for tests that need to exercise a real TLS handshake against qTest's
 * plugin HTTP client -- either to prove a self-signed/untrusted certificate is rejected (see
 * HttpClientUtilsTlsTest), or as a stand-in "qTest server" for submission-path tests that used
 * to point at a dead localhost:7443 (see JunitTestResultParserTests).
 */
public final class LocalHttpsFixture implements AutoCloseable {

  // Not a real secret: a throwaway password protecting a keystore that is (a) generated fresh
  // for each test run, (b) written to a JUnit @Rule TemporaryFolder deleted at test teardown,
  // and (c) never used outside this JVM. There is nothing here for this password to protect.
  private static final char[] EPHEMERAL_KEYSTORE_PASSWORD = "not-a-real-secret".toCharArray();

  private final HttpsServer server;
  private final int port;

  private LocalHttpsFixture(HttpsServer server) {
    this.server = server;
    this.port = server.getAddress().getPort();
  }

  public int getPort() {
    return port;
  }

  public String getBaseUrl() {
    return "https://localhost:" + port;
  }

  @Override
  public void close() {
    server.stop(0);
  }

  /**
   * Starts a server on a random free port. Every request to any path gets a 200 response with
   * the given body; callers that don't care about response content (most of these tests only
   * check that the plugin's HTTP client didn't throw) can pass a trivial body.
   */
  public static LocalHttpsFixture startRespondingWith(File tempDir, String responseBody) throws Exception {
    File keystoreFile = new File(tempDir, "self-signed-" + System.nanoTime() + ".p12");
    generateSelfSignedKeystore(keystoreFile);

    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream fis = new FileInputStream(keystoreFile)) {
      keyStore.load(fis, EPHEMERAL_KEYSTORE_PASSWORD);
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, EPHEMERAL_KEYSTORE_PASSWORD);
    SSLContext serverSslContext = SSLContext.getInstance("TLS");
    serverSslContext.init(kmf.getKeyManagers(), null, null);

    HttpsServer server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(serverSslContext));
    byte[] body = responseBody.getBytes();
    server.createContext("/", exchange -> {
      // Drain the request body fully before responding -- for large POSTs (e.g. bulk test-log
      // submissions), closing the exchange while the client is still writing the body causes a
      // broken-pipe error on the client side instead of a clean response.
      exchange.getRequestBody().readAllBytes();
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    return new LocalHttpsFixture(server);
  }

  private static void generateSelfSignedKeystore(File keystoreFile) throws Exception {
    Process process = new ProcessBuilder(
            System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool",
            "-genkeypair",
            "-alias", "test",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "1",
            "-keystore", keystoreFile.getAbsolutePath(),
            "-storetype", "PKCS12",
            "-storepass", new String(EPHEMERAL_KEYSTORE_PASSWORD),
            "-keypass", new String(EPHEMERAL_KEYSTORE_PASSWORD),
            "-dname", "CN=localhost",
            "-ext", "SAN=dns:localhost,ip:127.0.0.1"
    ).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes());
    int exit = process.waitFor();
    if (exit != 0) {
      throw new IllegalStateException("keytool failed (" + exit + "): " + output);
    }
  }
}
