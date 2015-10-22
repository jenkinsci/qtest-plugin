package com.qasymphony.ci.plugin.utils;

/**
 * @author trongle
 * @version 10/22/2015 8:16 PM trongle $
 * @since 1.0
 */
public class ResponseEntity {
  private String body;
  private Integer statusCode;

  public ResponseEntity(String body, Integer statusCode) {
    this.body = body;
    this.statusCode = statusCode;
  }

  public String getBody() {
    return body;
  }

  public Integer getStatusCode() {
    return statusCode;
  }
}
