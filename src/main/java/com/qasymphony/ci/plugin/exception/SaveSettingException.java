package com.qasymphony.ci.plugin.exception;

/**
 * @author trongle
 * @version 11/19/2015 3:21 PM trongle $
 * @since 1.0
 */
public class SaveSettingException extends Exception {
  private int status;

  public SaveSettingException() {
    super();
  }

  public SaveSettingException(String message) {
    super(message);
  }

  public SaveSettingException(String message, Throwable cause) {
    super(message, cause);
  }

  public SaveSettingException(String message, int status) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
