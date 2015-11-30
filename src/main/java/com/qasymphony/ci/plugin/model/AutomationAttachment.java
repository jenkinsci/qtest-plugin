package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author anpham
 * 
 */
public class AutomationAttachment {
  private String name;
  @JsonProperty("content_type")
  private String contentType;
  private String data;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

}
