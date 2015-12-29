package com.qasymphony.ci.plugin.utils;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * @author trongle
 * @version 11/23/2015 11:29 AM trongle $
 * @since 1.0
 */
public class LoggerUtils {
  private LoggerUtils() {

  }

  private static final String HR_TEXT = "------------------------------------------------------------------------";

  public static void formatHR(PrintStream logger) {
    formatInfo(logger, HR_TEXT);
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

  /**
   * Format duration
   *
   * @param start
   * @return
   */
  public static String elapsedTime(long start) {
    Long end = System.currentTimeMillis();
    Long duration = end - start;
    return String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(duration),
      TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
    );
  }
}
