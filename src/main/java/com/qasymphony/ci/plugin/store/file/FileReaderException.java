package com.qasymphony.ci.plugin.store.file;

/**
 * @author trongle
 * @version 10/23/2015 1:35 AM trongle $
 * @since 1.0
 */
public class FileReaderException extends  RuntimeException {
  public FileReaderException() {
    super();
  }

  public FileReaderException(String message) {
    super(message);
  }

  public FileReaderException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileReaderException(Throwable cause) {
    super(cause);
  }

  protected FileReaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
