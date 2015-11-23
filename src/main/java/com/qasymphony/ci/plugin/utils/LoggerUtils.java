package com.qasymphony.ci.plugin.utils;

import java.io.PrintStream;

/**
 * @author trongle
 * @version 11/23/2015 11:29 AM trongle $
 * @since 1.0
 */
public class LoggerUtils {
  private LoggerUtils() {

  }

  public static void formatInfo(PrintStream logger, String msg, Object... args) {
    format(logger, "INFO", msg, args);
  }

  public static void formatError(PrintStream logger, String msg, Object... args) {
    format(logger, "ERROR", msg, args);
  }

  public static void formatWarn(PrintStream logger, String msg, Object... args) {
    format(logger, "WARN", msg, args);
  }

  public static void format(PrintStream logger, String level, String msg, Object... args) {
    logger.println(String.format("[qTest] [%s] %s", level, String.format(msg, args)));
  }
}
