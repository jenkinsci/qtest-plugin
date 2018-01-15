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

  public Container setId(Long id) {
    this.id = id;
    return this;
  }

  public String getType() {
    return type;
  }

  public Container setType(String type) {
    this.type = type;
    return this;
  }

  public Boolean getCreateNewTestSuiteEveryBuild() {
    return createNewTestSuiteEveryBuild;
  }

  public Container setCreateNewTestSuiteEveryBuild(Boolean createNewTestSuiteEveryBuild) {
    this.createNewTestSuiteEveryBuild = createNewTestSuiteEveryBuild;
    return this;
  }

}
