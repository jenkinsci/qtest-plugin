package com.qasymphony.ci.plugin.exception;

/**
 * @author trongle
 * @version 11/2/2015 3:12 PM trongle $
 * @since 1.0
 */
public class SubmittedException extends Exception {
  private int status;

  public SubmittedException() {
    super();
  }

  public SubmittedException(String message) {
    super(message);
  }

  public SubmittedException(String message, int status) {
    super(message);
    this.status = status;
  }

  public SubmittedException(String message, Throwable cause) {
    super(message, cause);
  }

  public SubmittedException(Throwable cause) {
    super(cause);
  }

  protected SubmittedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public int getStatus() {
    return status;
  }

  public SubmittedException setStatus(int status) {
    this.status = status;
    return this;
  }
}
