package com.qasymphony.ci.plugin.support;

import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.exception.OAuthException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import hudson.tasks.junit.CaseResult;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class SubmitTestFixtures {

  private SubmitTestFixtures() {}

  public static Configuration ciConfiguration(QTestMockServerRule qtest) {
    return new Configuration(
      3L,
      qtest.baseUrl(),
      "3c76feb4-b91f-4a53-8643-bd1ce2f01a3e",
      1L,
      "TestPerformance",
      1L,
      "releaseName",
      0L,
      "environment",
      0L,
      0L,
      false,
      "",
      false,
      "{}",
      false,
      0);
  }

  public static String testSuiteContainerSetting(long suiteId) {
    return "{\"selectedContainer\":{\"name\":\"Test Suite\"},"
      + "\"containerPath\":\"[{\\\"nodeId\\\":" + suiteId + ",\\\"nodeType\\\":\\\"test-suite\\\"}]\"}";
  }

  public static String releaseContainerSetting(long releaseId) {
    return "{\"selectedContainer\":{\"name\":\"Release 1\"},"
      + "\"containerPath\":\"[{\\\"nodeId\\\":" + releaseId + ",\\\"nodeType\\\":\\\"release\\\"}]\"}";
  }

  public static String oauthAccessToken(QTestMockServerRule qtest, Configuration configuration)
    throws OAuthException {
    return OauthProvider.getAccessToken(qtest.baseUrl(), configuration.getAppSecretKey());
  }

  public static List<AutomationTestResult> singlePassedResult(String name) {
    AutomationTestResult result = new AutomationTestResult();
    result.setName(name);
    result.setAutomationContent(name);
    result.setStatus(CaseResult.Status.PASSED.toString());
    result.setExecutedStartDate(new Date());
    result.setExecutedEndDate(new Date());
    return Collections.singletonList(result);
  }

  public static void assertSubmitSuccess(ResponseEntity response) {
    assertNotNull(response);
    assertEquals(201, response.getStatusCode().intValue());
  }
}
