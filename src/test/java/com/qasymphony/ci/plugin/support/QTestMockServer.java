package com.qasymphony.ci.plugin.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Standalone WireMock server that stubs qTest Manager API endpoints for integration tests.
 */
public final class QTestMockServer implements AutoCloseable {

  private final WireMockServer wireMock;

  public QTestMockServer() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
  }

  public void start() {
    if (!wireMock.isRunning()) {
      wireMock.start();
    }
  }

  public void stop() {
    if (wireMock.isRunning()) {
      wireMock.stop();
    }
  }

  public String baseUrl() {
    return wireMock.baseUrl();
  }

  public WireMockServer getWireMock() {
    return wireMock;
  }

  public QTestMockServer stubOAuth() {
    wireMock.stubFor(post(urlPathMatching("/oauth/token"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"access_token\":\"test-access-token\"}")));
    return this;
  }

  public QTestMockServer stubCiAutoTestLogSubmit() {
    wireMock.stubFor(post(urlPathMatching("/api/v3/projects/[0-9]+/test-runs/0/auto-test-logs/ci/[0-9]+"))
      .willReturn(aResponse()
        .withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":1}")));
    return this;
  }

  public QTestMockServer stubContainerAutoTestLogSubmit() {
    wireMock.stubFor(post(urlPathMatching("/api/v3\\.1/projects/[0-9]+/test-runs/0/auto-test-logs"))
      .willReturn(aResponse()
        .withStatus(201)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":1}")));
    return this;
  }

  public QTestMockServer stubTaskStatus(long taskId) {
    wireMock.stubFor(get(urlPathMatching("/api/v3/projects/queue-processing/" + taskId))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":" + taskId + ",\"state\":\"SUCCESS\",\"content\":\"{\\\"testSuiteId\\\":1,\\\"testSuiteName\\\":\\\"Test Suite\\\",\\\"totalTestLogs\\\":1}\"}")));
    return this;
  }

  public void resetStubs() {
    wireMock.resetMappings();
  }

  public QTestMockServer stubOAuthFailure(int status) {
    wireMock.stubFor(post(urlPathMatching("/oauth/token"))
      .atPriority(10)
      .willReturn(aResponse().withStatus(status)));
    return this;
  }

  public QTestMockServer stubCiSubmitFailure(int status) {
    wireMock.stubFor(post(urlPathMatching("/api/v3/projects/[0-9]+/test-runs/0/auto-test-logs/ci/[0-9]+"))
      .atPriority(10)
      .willReturn(aResponse().withStatus(status)));
    return this;
  }

  public QTestMockServer stubTestSuiteChildren(String jsonBody) {
    wireMock.stubFor(get(urlPathMatching("/api/v3/projects/[0-9]+/test-suites.*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(jsonBody)));
    return this;
  }

  public QTestMockServer stubCreateTestSuite(long suiteId) {
    wireMock.stubFor(post(urlPathMatching("/api/v3/projects/[0-9]+/test-suites.*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":" + suiteId + "}")));
    return this;
  }

  public void verifyOAuthCalledOnce() {
    verifyOAuthCalled(1);
  }

  public void verifyOAuthCalled(int count) {
    wireMock.verify(count, postRequestedFor(urlPathMatching("/oauth/token")));
  }

  public void verifyOAuthCalledAtLeast(int minimum) {
    int actual = wireMock.findAll(postRequestedFor(urlPathMatching("/oauth/token"))).size();
    if (actual < minimum) {
      throw new com.github.tomakehurst.wiremock.client.VerificationException(
        "Expected at least " + minimum + " OAuth requests but received " + actual);
    }
  }

  public QTestMockServer stubVersion() {
    wireMock.stubFor(get(urlPathMatching("/version"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"name\":\"test-conductor\",\"version\":\"10.0.0\"}")));
    return this;
  }

  public QTestMockServer stubCiConfigurationLookup() {
    wireMock.stubFor(get(urlPathMatching("/api/v3/projects/[0-9]+/ci.*"))
      .willReturn(aResponse().withStatus(404)));
    return this;
  }

  public QTestMockServer stubSaveCiConfiguration() {
    wireMock.stubFor(post(urlPathMatching("/api/v3/projects/[0-9]+/ci"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":3,\"moduleId\":0,\"projectId\":1,\"ciType\":\"jenkins\"}")));
    return this;
  }

  public QTestMockServer stubProjectInfo() {
    wireMock.stubFor(get(urlPathMatching("/api/v3/projects/[0-9]+"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":1,\"name\":\"Test Project\"}")));
    return this;
  }

  public QTestMockServer stubReleaseInfo() {
    wireMock.stubFor(get(urlPathMatching("/api/v3/projects/[0-9]+/releases/[0-9]+"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":1,\"name\":\"Release 1\"}")));
    return this;
  }

  public QTestMockServer stubPipelineSupport() {
    return stubVersion()
      .stubCiConfigurationLookup()
      .stubSaveCiConfiguration()
      .stubProjectInfo()
      .stubReleaseInfo();
  }

  public void verifyCreateTestSuiteCalledOnce() {
    wireMock.verify(1, postRequestedFor(urlPathMatching(
      "/api/v3/projects/[0-9]+/test-suites.*")));
  }

  public void verifyTestSuiteChildrenQueried() {
    wireMock.verify(getRequestedFor(urlPathMatching(
      "/api/v3/projects/[0-9]+/test-suites.*")));
  }

  public void verifyCiSubmitCalledOnce() {
    wireMock.verify(1, postRequestedFor(urlPathMatching(
      "/api/v3/projects/[0-9]+/test-runs/0/auto-test-logs/ci/[0-9]+")));
  }

  public void verifyContainerSubmitCalledOnce() {
    wireMock.verify(1, postRequestedFor(urlPathMatching(
      "/api/v3\\.1/projects/[0-9]+/test-runs/0/auto-test-logs")));
  }

  public void verifyCiSubmitBodyContains(String fragment) {
    wireMock.verify(postRequestedFor(urlPathMatching(
      "/api/v3/projects/[0-9]+/test-runs/0/auto-test-logs/ci/[0-9]+"))
      .withRequestBody(containing(fragment)));
  }

  public void verifyContainerSubmitBodyContains(String fragment) {
    wireMock.verify(postRequestedFor(urlPathMatching(
      "/api/v3\\.1/projects/[0-9]+/test-runs/0/auto-test-logs"))
      .withRequestBody(containing(fragment)));
  }

  public QTestMockServer stubDefaults() {
    return stubOAuth()
      .stubCiAutoTestLogSubmit()
      .stubContainerAutoTestLogSubmit()
      .stubTaskStatus(1);
  }

  @Override
  public void close() {
    stop();
  }
}
