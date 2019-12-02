package com.qasymphony.ci.plugin;

import java.util.Arrays;
import java.util.List;

/**
 * @author trongle
 * @version 12/29/2015 2:52 PM trongle $
 * @since 1.0
 */
public class Constants {
  private Constants() {
  }

  public static final String HEADER_AUTH = "Authorization";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String CONTENT_TYPE_ZIP = "application/zip";
  public static final String CONTENT_TYPE_TEXT = "text/plain";

  public static final String CI_TYPE = "jenkins";
  /**
   * Retry interval for get task status in 3 second
   */
  public static final Integer RETRY_INTERVAL = 3000;
  // Stop retrying get task status in 1 hour
  public static final Integer MAX_RETRY_TIMEOUT = 3600000;
  /**
   * State list which marked as submission have been finished
   */
  public static final List<String> LIST_FINISHED_STATE = Arrays.asList("SUCCESS", "FAILED");

  public static final String JENKINS_SERVER_ID_DEFAULT = "UNKNOWN_JENKINS_SERVER_ID";

  public static final class Extension {
    public static final String TEXT_FILE = ".txt";
    public static final String ZIP_FILE = ".zip";
  }

  public static final Integer LIMIT_TXT_FILES = 5;
  public static final String JUNIT_PREFIX = "TEST-*";
  public static final String JUNIT_SUFFIX = "/*.xml";

  /**
   * Support submit test logs for qTest version from 8.9.4 or less
   */
  public static final String OLD_QTEST_VERSION = "8.9.4";

  public static final String PROJECT_NAME = "project_name";
  public static final String CONTAINER_NAME = "container_name";
  public static final String CONFIGURATION_ID = "configuration_id";
  public static final String ENVIRONMENT_NAME = "environment_name";

  public static final class TestResultStatus {
    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";
    public static final String SKIP = "SKIP";
    public static final String PASSED = "PASSED";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";
    public static final String ERROR = "ERROR";
  }
}
