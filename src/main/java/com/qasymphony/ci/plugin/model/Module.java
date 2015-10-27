/**
 * 
 */
package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author anpham
 * 
 */
public class Module {
  private String name;
  private long id;
  private int order;
  @JsonProperty("parent_id")
  private long parentId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public long getParentId() {
    return parentId;
  }

  public void setParentId(long parentId) {
    this.parentId = parentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
