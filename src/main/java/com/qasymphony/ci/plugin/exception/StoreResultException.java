package com.qasymphony.ci.plugin.exception;

/**
 * @author trongle
 * @version 10/28/2015 11:38 PM trongle $
 * @since 1.0
 */
public class StoreResultException extends Exception {
  public StoreResultException() {
    super();
  }

  public StoreResultException(String message) {
    super(message);
  }

  public StoreResultException(String message, Throwable cause) {
    super(message, cause);
  }

  public StoreResultException(Throwable cause) {
    super(cause);
  }

  protected StoreResultException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
