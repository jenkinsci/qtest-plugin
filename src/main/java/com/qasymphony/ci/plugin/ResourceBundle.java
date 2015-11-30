/**
 *
 */
package com.qasymphony.ci.plugin;

import org.jvnet.localizer.ResourceBundleHolder;

/**
 * @author anpham
 */
public class ResourceBundle {
  private static final ResourceBundle INSTANCE = new ResourceBundle();
  private final ResourceBundleHolder holder;
  public static final String DISPLAY_NAME = get("displayName");
  public static final String CONFIG_HELP_FILE = get("config.help.file");
  public static final String MSG_INVALID_URL = get("validation.invalid.url");
  public static final String MSG_INVALID_API_KEY = get("validation.invalid.api.key");
  public static final String MSG_INVALID_PROJECT = get("validation.invalid.project");
  public static final String MSG_INVALID_RELEASE = get("validation.invalid.release");

  private ResourceBundle() {
    holder = ResourceBundleHolder.get(this.getClass());
  }

  public static String get(String key, Object... args) {
    return INSTANCE.holder.format(key, args);
  }
}