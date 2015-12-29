package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.utils.JsonUtils;

import java.io.IOException;

/**
 * @author anpham
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutomationTestResponse {
  private long id;
  private String type;
  private String state;
  private String contentType;
  private String testSuiteName;
  private long testSuiteId;
  private int totalTestLogs;
  private String content;
  private Boolean hasError = false;

  public AutomationTestResponse() {
  }

  public AutomationTestResponse(String body) {
    JsonNode node = JsonUtils.readTree(body);
    this.id = JsonUtils.getLong(node, "id");
    this.type = JsonUtils.getText(node, "type");
    this.state = JsonUtils.getText(node, "state");
    this.contentType = JsonUtils.getText(node, "contentType");
    content = JsonUtils.getText(node, "content");
    JsonNode contentNode = null;
    try {
      contentNode = JsonUtils.parseTree(content);
    } catch (IOException e) {
      hasError = true;
    }
    if (null != contentNode) {
      this.testSuiteId = JsonUtils.getLong(contentNode, "testSuiteId");
      this.testSuiteName = JsonUtils.getText(contentNode, "testSuiteName");
      this.totalTestLogs = JsonUtils.getInt(contentNode, "totalTestLogs");
    }
  }

  public Boolean hasError() {
    return hasError;
  }

  public long getId() {
    return id;
  }

  public AutomationTestResponse setId(long id) {
    this.id = id;
    return this;
  }

  public String getType() {
    return type;
  }

  public AutomationTestResponse setType(String type) {
    this.type = type;
    return this;
  }

  public String getState() {
    return state;
  }

  public AutomationTestResponse setState(String state) {
    this.state = state;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public AutomationTestResponse setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public void setTestSuiteName(String testSuiteName) {
    this.testSuiteName = testSuiteName;
  }

  public long getTestSuiteId() {
    return testSuiteId;
  }

  public void setTestSuiteId(long testSuiteId) {
    this.testSuiteId = testSuiteId;
  }

  public int getTotalTestLogs() {
    return totalTestLogs;
  }

  public void setTotalTestLogs(int totalTestLogs) {
    this.totalTestLogs = totalTestLogs;
  }

  public String getContent() {
    return content;
  }

  public AutomationTestResponse setContent(String content) {
    this.content = content;
    return this;
  }
}
