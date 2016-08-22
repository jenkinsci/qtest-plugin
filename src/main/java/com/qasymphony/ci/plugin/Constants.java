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
   * Retry interval for get task status in 1 second
   */
  public static final Integer RETRY_INTERVAL = 2000;

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
}
