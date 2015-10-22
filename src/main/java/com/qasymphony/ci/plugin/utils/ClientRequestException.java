package com.qasymphony.ci.plugin.utils;

/**
 * @author trongle
 * @version 10/22/2015 8:14 PM trongle $
 * @since 1.0
 */
public class ClientRequestException extends Exception {
  public ClientRequestException() {
    super();
  }

  public ClientRequestException(String message) {
    super(message);
  }

  public ClientRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
