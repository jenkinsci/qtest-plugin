package com.qasymphony.ci.plugin.utils;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Regression test for CWE-113 (Improper Neutralization of CRLF Sequences in HTTP Headers),
 * surfaced by Snyk in ConfigService, qTestService, SubmitJUnitStep, and JunitQtestSubmitterImpl
 * -- all of which build header values from config-provided strings (server URL, project name,
 * etc.) and funnel them through HttpClientUtils.addHeader with no sanitization.
 */
public class HttpClientUtilsHeaderInjectionTest {

  @Test
  public void stripCrlfRemovesEmbeddedCarriageReturnsAndNewlines() {
    assertEquals("EvilValueSet-Cookie:evil=1",
            HttpClientUtils.stripCrlf("EvilValue\r\nSet-Cookie:evil=1"));
    assertNull(HttpClientUtils.stripCrlf(null));
    assertEquals("plain", HttpClientUtils.stripCrlf("plain"));
  }

  @Test
  public void addHeaderSanitizesInjectedCrlfBeforeSettingTheRequestHeader() {
    HttpGet request = new HttpGet("https://example.invalid/");
    Map<String, String> headers = new HashMap<>();
    headers.put("X-Test", "value\r\nX-Injected: true");

    HttpClientUtils.addHeader(request, headers);

    assertEquals("valueX-Injected: true", request.getFirstHeader("X-Test").getValue());
    assertNull("the injected header must not have been split out on its own",
            request.getFirstHeader("X-Injected"));
  }
}
