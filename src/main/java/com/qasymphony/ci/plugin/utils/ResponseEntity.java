package com.qasymphony.ci.plugin.utils;

/**
 * @author trongle
 * @version 10/22/2015 8:16 PM trongle $
 * @since 1.0
 */
public class ResponseEntity {
  private final String body;
  private final Integer statusCode;

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

  @Override public String toString() {
    return "ResponseEntity{" +
      "body='" + body + '\'' +
      ", statusCode=" + statusCode +
      '}';
  }
}
