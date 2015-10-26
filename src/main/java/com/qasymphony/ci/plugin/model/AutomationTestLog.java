/**
 * 
 */
package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author anpham
 * 
 */
public class AutomationTestLog {
  @JsonProperty("description")
  private String description;
  @JsonProperty("expected_result")
  private String expectedResult;
  private Integer order;
  private String status;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExpectedResult() {
    return expectedResult;
  }

  public void setExpectedResult(String expectedResult) {
    this.expectedResult = expectedResult;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
