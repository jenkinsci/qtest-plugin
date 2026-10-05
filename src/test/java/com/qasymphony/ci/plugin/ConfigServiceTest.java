package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

/**
 * ConfigService is the heaviest HttpClientUtils consumer in the plugin (14 call sites) and had
 * zero coverage before this pass. Covers the validation methods (used directly by
 * ValidationFormService, already covered separately) plus a representative sample of the
 * HTTP-wrapper methods that share the same shape (build URL, call HttpClientUtils, parse/guard
 * the response) -- not all ~20 public methods, since most are structurally identical to the
 * ones covered here.
 */
public class ConfigServiceTest {

  @Test
  public void validateQtestUrlTrueOnlyWhenNameMatchesTestConductor() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(eq("https://qtest.example/version"), isNull()))
              .thenReturn(new ResponseEntity("{\"name\":\"test-conductor\"}", 200));

      assertTrue(ConfigService.validateQtestUrl("https://qtest.example"));
    }
  }

  @Test
  public void validateQtestUrlFalseWhenNameDoesNotMatch() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), isNull()))
              .thenReturn(new ResponseEntity("{\"name\":\"some-other-app\"}", 200));

      assertFalse(ConfigService.validateQtestUrl("https://not-qtest.example"));
    }
  }

  @Test
  public void validateQtestUrlFalseWhenServerUnreachable() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), isNull()))
              .thenThrow(new ClientRequestException("connection refused"));

      assertFalse(ConfigService.validateQtestUrl("https://qtest.example"));
    }
  }

  @Test
  public void getQtestInfoReturnsBodyOrNullOnFailure() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(eq("https://qtest.example/version"), isNull()))
              .thenReturn(new ResponseEntity("{\"version\":\"1.0\"}", 200));
      assertEquals("{\"version\":\"1.0\"}", ConfigService.getQtestInfo("https://qtest.example"));
    }

    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), isNull()))
              .thenThrow(new ClientRequestException("timeout"));
      assertNull(ConfigService.getQtestInfo("https://qtest.example"));
    }
  }

  @Test
  public void validateApiKeyTrueOnlyWhenTokenExchangeSucceeds() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), any(), isNull()))
              .thenReturn(new ResponseEntity("{\"access_token\":\"abc\"}", 200));

      assertTrue(ConfigService.validateApiKey("https://qtest.example", "key", "secret"));
    }

    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), any(), isNull()))
              .thenReturn(new ResponseEntity("unauthorized", 401));

      assertFalse(ConfigService.validateApiKey("https://qtest.example", "bad-key", "secret"));
    }
  }

  @Test
  public void validateSecretKeyRequiresBasicPrefix() {
    assertTrue(ConfigService.validateSecretKey("Basic dXNlcjpwYXNz"));
    assertFalse(ConfigService.validateSecretKey("dXNlcjpwYXNz"));
    assertFalse(ConfigService.validateSecretKey(""));
    assertFalse(ConfigService.validateSecretKey(null));
  }

  @Test
  public void formatTestSuiteLinkBuildsExpectedUrl() {
    assertEquals(
            "https://qtest.example/p/1/portal/project#tab=testexecution&object=2&id=2",
            ConfigService.formatTestSuiteLink("https://qtest.example", 1L, 2L));
  }

  @Test
  public void getProjectsReturnsNullOnNonOkStatusOrFailure() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class);
         MockedStatic<OauthProvider> oauth = mockStatic(OauthProvider.class)) {
      oauth.when(() -> OauthProvider.buildHeaders(any(), any(), any(), any())).thenReturn(java.util.Map.of());
      mocked.when(() -> HttpClientUtils.get(any(), any())).thenReturn(new ResponseEntity("forbidden", 403));

      assertNull(ConfigService.getProjects("https://qtest.example", "key", "secret"));
    }
  }

  @Test
  public void getProjectsReturnsBodyOnOkStatus() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class);
         MockedStatic<OauthProvider> oauth = mockStatic(OauthProvider.class)) {
      oauth.when(() -> OauthProvider.buildHeaders(any(), any(), any(), any())).thenReturn(java.util.Map.of());
      mocked.when(() -> HttpClientUtils.get(eq("https://qtest.example/api/v3/projects?assigned=true"), any()))
              .thenReturn(new ResponseEntity("[{\"id\":1}]", 200));

      assertEquals("[{\"id\":1}]", ConfigService.getProjects("https://qtest.example", "key", "secret"));
    }
  }

  @Test
  public void getErrorMessageParsesErrorBodyOrFallsBackToRawBody() {
    assertEquals("something went wrong",
            ConfigService.getErrorMessage("{\"message\":\"something went wrong\"}"));
    // not valid JSON for the Error model -- falls back to returning the raw body unchanged
    assertEquals("not json", ConfigService.getErrorMessage("not json"));
  }

  @Test
  public void compareqTestVersionTrueWhenInstalledVersionIsOlder() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), isNull()))
              .thenReturn(new ResponseEntity("{\"version\":\"1.0\"}", 200));

      assertTrue(ConfigService.compareqTestVersion("https://qtest.example", "2.0"));
    }
  }

  @Test
  public void compareqTestVersionFalseWhenQtestInfoUnavailable() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), isNull()))
              .thenThrow(new ClientRequestException("timeout"));

      assertFalse(ConfigService.compareqTestVersion("https://qtest.example", "2.0"));
    }
  }
}
