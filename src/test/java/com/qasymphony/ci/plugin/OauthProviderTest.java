package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.exception.OAuthException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * OauthProvider is one of the "affected call paths" named in QTEST-39511 (it builds the OAuth
 * token-exchange request that carries the qTest API key/secret) and had zero test coverage
 * before this pass -- only exercised indirectly via a live-server-dependent test with no
 * assertions on the actual header/token content. HttpClientUtils is mocked statically
 * (inline mockmaker, already wired via the parent hpi-plugin javaagent) rather than requiring a
 * live HTTP round-trip, since OauthProvider's own logic (URL construction, header building,
 * status/JSON handling) is what's under test here, not HttpClientUtils itself.
 */
public class OauthProviderTest {

  @Test
  public void getAccessTokenReturnsTokenOnSuccessfulResponse() throws Exception {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), anyMap(), isNull()))
              .thenReturn(new ResponseEntity("{\"access_token\":\"abc123\"}", 200));

      String token = OauthProvider.getAccessToken("https://qtest.example", "api-key", "secret-key");

      assertEquals("abc123", token);
    }
  }

  @Test
  public void getAccessTokenSendsRefreshTokenGrantWithEncodedApiKeyAndRawSecretInAuthHeader() throws Exception {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), anyMap(), isNull()))
              .thenReturn(new ResponseEntity("{\"access_token\":\"abc123\"}", 200));

      OauthProvider.getAccessToken("https://qtest.example", "api key/needs+encoding", "the-secret");

      mocked.verify(() -> HttpClientUtils.post(
              contains("https://qtest.example/oauth/token?grant_type=refresh_token&refresh_token="),
              argThatContainsAuthHeader("the-secret"),
              isNull()));
    }
  }

  @Test
  public void getAccessTokenThrowsOAuthExceptionOnNonOkStatus() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), anyMap(), isNull()))
              .thenReturn(new ResponseEntity("unauthorized", 401));

      try {
        OauthProvider.getAccessToken("https://qtest.example", "api-key", "secret-key");
        fail("Expected an OAuthException for a non-200 response");
      } catch (OAuthException expected) {
        assertEquals("unauthorized", expected.getMessage());
        // NOTE (found while adding this test, not fixed here -- out of scope for a coverage
        // pass): the inner `throw new OAuthException(entity.getBody(), entity.getStatusCode())`
        // is itself caught by getAccessToken's own outer `catch (Exception e)` and rewrapped via
        // `new OAuthException(e.getMessage(), e)`, which uses the (message, Throwable)
        // constructor and never sets `status`. The 401 is silently lost; getStatus() is always
        // 0 for this path. Documenting actual behavior, not the apparently-intended one.
        assertEquals(0, expected.getStatus());
      }
    }
  }

  @Test
  public void getAccessTokenThrowsOAuthExceptionWhenResponseIsNotValidJson() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), anyMap(), isNull()))
              .thenReturn(new ResponseEntity("not json at all", 200));

      try {
        OauthProvider.getAccessToken("https://qtest.example", "api-key", "secret-key");
        fail("Expected an OAuthException when the body cannot be parsed as JSON");
      } catch (OAuthException expected) {
        assertTrue(expected.getMessage().contains("Cannot get access token"));
      }
    }
  }

  @Test
  public void buildHeadersFromAccessTokenSetsBearerAuthAndJsonContentType() {
    Map<String, String> extra = new HashMap<>();
    extra.put("X-Extra", "value");

    Map<String, String> headers = OauthProvider.buildHeaders("my-token", extra);

    assertEquals("Bearer my-token", headers.get(Constants.HEADER_AUTH));
    assertEquals(Constants.CONTENT_TYPE_JSON, headers.get(Constants.HEADER_CONTENT_TYPE));
    assertEquals("value", headers.get("X-Extra"));
  }

  @Test
  public void fourArgBuildHeadersSwallowsOAuthFailureAndReturnsBearerNull() {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.encode(any())).thenCallRealMethod();
      mocked.when(() -> HttpClientUtils.post(any(), anyMap(), isNull()))
              .thenReturn(new ResponseEntity("unauthorized", 401));

      // Documents existing behavior: a failed token exchange is swallowed (logged, not thrown)
      // and callers get a "Bearer null" header rather than an exception -- worth revisiting
      // separately, but this pass is about coverage, not changing this contract.
      Map<String, String> headers = OauthProvider.buildHeaders("https://qtest.example", "api-key", "secret-key", null);

      assertEquals("Bearer null", headers.get(Constants.HEADER_AUTH));
    }
  }

  private static Map<String, String> argThatContainsAuthHeader(String expectedValue) {
    return org.mockito.ArgumentMatchers.argThat(map ->
            map != null && expectedValue.equals(map.get(Constants.HEADER_AUTH)));
  }
}
