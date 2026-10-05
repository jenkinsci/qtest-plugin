package com.qasymphony.ci.plugin;

import hudson.util.FormValidation;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * ValidationFormService is "the only input validation in the plugin" (per the source-usage
 * analysis) and had zero coverage before this pass. ConfigService is mocked statically (its own
 * validate* methods make real HTTP calls) so this test exercises ValidationFormService's own
 * branching, not the network.
 */
public class ValidationFormServiceTest {

  @Test
  public void checkUrlErrorsOnEmptyValue() throws Exception {
    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkUrl("", null).kind);
    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkUrl(null, null).kind);
  }

  @Test
  public void checkUrlErrorsOnMalformedUrl() throws Exception {
    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkUrl("not a url", null).kind);
  }

  @Test
  public void checkUrlOkOnlyWhenConfigServiceConfirmsItIsAQtestUrl() throws Exception {
    try (MockedStatic<ConfigService> mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.validateQtestUrl(eq("https://qtest.example"))).thenReturn(true);
      mocked.when(() -> ConfigService.validateQtestUrl(eq("https://not-qtest.example"))).thenReturn(false);

      assertEquals(FormValidation.Kind.OK, ValidationFormService.checkUrl("https://qtest.example", null).kind);
      assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkUrl("https://not-qtest.example", null).kind);
    }
  }

  @Test
  public void checkUrlErrorsWhenConfigServiceThrows() throws Exception {
    try (MockedStatic<ConfigService> mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.validateQtestUrl(any())).thenThrow(new RuntimeException("boom"));

      assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkUrl("https://qtest.example", null).kind);
    }
  }

  @Test
  public void checkAppSecretKeyErrorsOnEmptyValueOrUrlWithoutCallingConfigService() throws Exception {
    try (MockedStatic<ConfigService> mocked = mockStatic(ConfigService.class)) {
      assertEquals(FormValidation.Kind.ERROR,
              ValidationFormService.checkAppSecretKey("", "https://qtest.example", "secret", null).kind);
      assertEquals(FormValidation.Kind.ERROR,
              ValidationFormService.checkAppSecretKey("key", "", "secret", null).kind);

      mocked.verifyNoInteractions();
    }
  }

  @Test
  public void checkAppSecretKeyDelegatesToConfigServiceWhenValuesPresent() throws Exception {
    try (MockedStatic<ConfigService> mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.validateApiKey("https://qtest.example", "good-key", "secret")).thenReturn(true);
      mocked.when(() -> ConfigService.validateApiKey("https://qtest.example", "bad-key", "secret")).thenReturn(false);

      assertEquals(FormValidation.Kind.OK,
              ValidationFormService.checkAppSecretKey("good-key", "https://qtest.example", "secret", null).kind);
      assertEquals(FormValidation.Kind.ERROR,
              ValidationFormService.checkAppSecretKey("bad-key", "https://qtest.example", "secret", null).kind);
    }
  }

  @Test
  public void checkSecretKeyDelegatesToConfigService() {
    try (MockedStatic<ConfigService> mocked = mockStatic(ConfigService.class)) {
      mocked.when(() -> ConfigService.validateSecretKey("valid")).thenReturn(true);
      mocked.when(() -> ConfigService.validateSecretKey("invalid")).thenReturn(false);

      assertEquals(FormValidation.Kind.OK, ValidationFormService.checkSecretKey("valid", null).kind);
      assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkSecretKey("invalid", null).kind);
    }
  }

  @Test
  public void blankRequiredFieldsAllErrorAndNonBlankAllOk() throws Exception {
    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkProjectName(" ").kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkProjectName("proj").kind);

    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkReleaseName("").kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkReleaseName("rel").kind);

    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkFakeContainerName(null).kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkFakeContainerName("container").kind);

    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkExternalCommand("").kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkExternalCommand("cmd").kind);

    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkExternalArguments(" ").kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkExternalArguments("args").kind);

    assertEquals(FormValidation.Kind.ERROR, ValidationFormService.checkExternalPathToResults("").kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkExternalPathToResults("path").kind);
  }

  @Test
  public void environmentAndResultPatternAreAlwaysOk() throws Exception {
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkEnvironment(null).kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkEnvironment("anything").kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkResultPattern(null).kind);
    assertEquals(FormValidation.Kind.OK, ValidationFormService.checkResultPattern("**/*.xml").kind);
  }
}
