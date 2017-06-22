package com.qasymphony.ci.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qasymphony.ci.plugin.Constants;
import hudson.tasks.junit.CaseResult;
import org.apache.commons.lang.StringUtils;

/**
 * @author anpham
 */
public class AutomationAttachment {
  private String name;
  @JsonProperty("content_type")
  private String contentType;
  private String data;

  public AutomationAttachment() {
  }

  public AutomationAttachment(CaseResult caseResult) {
    this.setName(caseResult.getSafeName().concat(Constants.Extension.TEXT_FILE));
    this.setContentType(Constants.CONTENT_TYPE_TEXT);
    this.setData(StringUtils.isEmpty(caseResult.getErrorStackTrace()) ? caseResult.getErrorDetails() : caseResult.getErrorStackTrace());
  }

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
