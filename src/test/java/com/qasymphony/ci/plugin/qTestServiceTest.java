package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * Covers qTestService's two direct HttpClientUtils.get consumers (getContainerInfo,
 * getProjectInfo), which had zero coverage before this pass. getContainerChildren/getProjectData
 * are deliberately out of scope here: they're multi-threaded orchestrators (ExecutorService +
 * CountDownLatch) that only fan out to ConfigService methods, not additional HttpClientUtils
 * call sites -- lower marginal coverage value for the effort of testing thread orchestration.
 */
public class qTestServiceTest {

  @Test
  public void getContainerInfoBuildsReleaseUrlAndParsesResponse() throws Exception {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      Map<String, String> headers = new HashMap<>();
      mocked.when(() -> HttpClientUtils.get(
              eq("https://qtest.example/api/v3/projects/1/releases/2"), anyMap()))
              .thenReturn(new ResponseEntity("{\"id\":2,\"name\":\"Release 1\"}", 200));

      JSONObject result = qTestService.getContainerInfo("https://qtest.example", headers, 1L, "release", 2L);

      assertEquals(2, result.getInt("id"));
      assertEquals("Release 1", result.getString("name"));
    }
  }

  @Test
  public void getContainerInfoMapsContainerTypeToUrlSegment() throws Exception {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), anyMap()))
              .thenReturn(new ResponseEntity("{}", 200));

      qTestService.getContainerInfo("https://qtest.example", new HashMap<>(), 1L, "test-suite", 2L);
      mocked.verify(() -> HttpClientUtils.get(
              eq("https://qtest.example/api/v3/projects/1/test-suites/2"), anyMap()));

      qTestService.getContainerInfo("https://qtest.example", new HashMap<>(), 1L, "test-cycle", 2L);
      mocked.verify(() -> HttpClientUtils.get(
              eq("https://qtest.example/api/v3/projects/1/test-cycles/2"), anyMap()));

      // any unrecognized type falls back to "releases" -- documenting actual behavior
      qTestService.getContainerInfo("https://qtest.example", new HashMap<>(), 1L, "something-else", 2L);
      mocked.verify(() -> HttpClientUtils.get(
              eq("https://qtest.example/api/v3/projects/1/releases/2"), anyMap()));
    }
  }

  @Test(expected = ClientRequestException.class)
  public void getContainerInfoPropagatesHttpClientFailure() throws Exception {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(any(), anyMap()))
              .thenThrow(new ClientRequestException("connection refused"));

      qTestService.getContainerInfo("https://qtest.example", new HashMap<>(), 1L, "release", 2L);
    }
  }

  @Test
  public void getProjectInfoBuildsProjectUrlAndParsesResponse() throws Exception {
    try (MockedStatic<HttpClientUtils> mocked = mockStatic(HttpClientUtils.class)) {
      mocked.when(() -> HttpClientUtils.get(
              eq("https://qtest.example/api/v3/projects/42"), anyMap()))
              .thenReturn(new ResponseEntity("{\"id\":42,\"name\":\"My Project\"}", 200));

      JSONObject result = qTestService.getProjectInfo("https://qtest.example", new HashMap<>(), 42L);

      assertEquals(42, result.getInt("id"));
      assertEquals("My Project", result.getString("name"));
    }
  }

  @Test
  public void getQtestInfoWrapsConfigServiceResultOrReturnsNull() {
    try (var mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.getQtestInfo("https://qtest.example"))
              .thenReturn("{\"version\":\"1.0\"}");

      JSONObject result = qTestService.getQtestInfo("https://qtest.example");
      assertTrue(result.getJSONObject("qTestInfo").containsKey("version"));
    }

    try (var mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.getQtestInfo(any())).thenReturn(null);
      assertEquals(null, qTestService.getQtestInfo("https://qtest.example"));
    }

    try (var mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.getQtestInfo(any())).thenThrow(new RuntimeException("boom"));
      // exceptions are swallowed (documenting actual behavior) -- returns null, doesn't throw
      assertEquals(null, qTestService.getQtestInfo("https://qtest.example"));
    }
  }
}
