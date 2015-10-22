/**
 * 
 */
package com.qasymphony.ci.plugin;

import org.jvnet.localizer.ResourceBundleHolder;

/**
 * @author anpham
 *
 */
public enum ResourceBundle {
  INSTANCE;
  
  public static final String DISPLAY_NAME = "displayName";
  private final ResourceBundleHolder holder;
  
  ResourceBundle(){
    holder = ResourceBundleHolder.get(this.getClass());
  }
  
  public String get(String key, Object ...args){
    return holder.format(key, args);
  }
}