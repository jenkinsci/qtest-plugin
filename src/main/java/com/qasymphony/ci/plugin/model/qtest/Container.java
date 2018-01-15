package com.qasymphony.ci.plugin.model.qtest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Container {
  @JsonProperty("id")
  private Long id;

  @JsonProperty("type")
  private String type;

  @JsonProperty("create_new_test_suite")
  private Boolean createNewTestSuiteEveryBuild;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Boolean getCreateNewTestSuiteEveryBuild() {
    return createNewTestSuiteEveryBuild;
  }

  public void setCreateNewTestSuiteEveryBuild(Boolean createNewTestSuiteEveryBuild) {
    this.createNewTestSuiteEveryBuild = createNewTestSuiteEveryBuild;
  }

}
