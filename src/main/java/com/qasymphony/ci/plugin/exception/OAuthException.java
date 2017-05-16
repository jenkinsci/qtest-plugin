package com.qasymphony.ci.plugin.exception;

/**
 * @author trongle
 * @version 11/19/2015 3:21 PM trongle $
 * @since 1.0
 */
public class OAuthException extends Exception {
  private int status;

  public OAuthException() {
    super();
  }

  public OAuthException(String message) {
    super(message);
  }

  public OAuthException(String message, Throwable cause) {
    super(message, cause);
  }

  public OAuthException(String message, int status) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
