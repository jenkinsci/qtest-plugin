package com.qasymphony.ci.plugin.model.qtest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author trongle
 * @version 12/24/2015 12:25 AM trongle $
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmittedTask {
  private long id;
  private String type;
  private String state;

  public long getId() {
    return id;
  }

  public SubmittedTask setId(long id) {
    this.id = id;
    return this;
  }

  public String getType() {
    return type;
  }

  public SubmittedTask setType(String type) {
    this.type = type;
    return this;
  }

  public String getState() {
    return state;
  }

  public SubmittedTask setState(String state) {
    this.state = state;
    return this;
  }
}
