/**
 *
 */
package com.qasymphony.ci.plugin;

import org.jvnet.localizer.ResourceBundleHolder;

/**
 * @author anpham
 */
public class ResourceBundle {
  public static final String GLOBAL_ERROR_MESSAGE = "global.error.message";
  private static final ResourceBundle INSTANCE = new ResourceBundle();
  private final ResourceBundleHolder holder;
  public static final String DISPLAY_NAME = get("displayName");

  private ResourceBundle() {
    holder = ResourceBundleHolder.get(this.getClass());
  }

  public static String get(String key, Object... args) {
    return INSTANCE.holder.format(key, args);
  }
}