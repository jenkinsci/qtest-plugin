/**
 *
 */
package com.qasymphony.ci.plugin;

import org.jvnet.localizer.ResourceBundleHolder;

/**
 * @author anpham
 */
public class ResourceBundle {
  public static final String DISPLAY_NAME = "displayName";
  private final ResourceBundleHolder holder;

  private ResourceBundle() {
    holder = ResourceBundleHolder.get(this.getClass());
  }

  private static ResourceBundle INSTANCE = new ResourceBundle();

  public static String get(String key, Object... args) {
    return INSTANCE.holder.format(key, args);
  }
}