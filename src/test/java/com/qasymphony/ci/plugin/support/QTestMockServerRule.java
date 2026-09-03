package com.qasymphony.ci.plugin.support;

import org.junit.rules.ExternalResource;

/**
 * JUnit 4 rule that starts a {@link QTestMockServer} with default qTest API stubs.
 */
public class QTestMockServerRule extends ExternalResource {

  private final QTestMockServer server = new QTestMockServer();

  public String baseUrl() {
    return server.baseUrl();
  }

  public QTestMockServer getServer() {
    return server;
  }

  @Override
  protected void before() {
    server.start();
    server.stubDefaults();
  }

  @Override
  protected void after() {
    server.stop();
  }
}
