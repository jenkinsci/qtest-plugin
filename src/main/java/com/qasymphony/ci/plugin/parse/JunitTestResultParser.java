package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author trongle
 * @version 11/2/2015 9:13 AM trongle $
 * @since 1.0
 */
public class JunitTestResultParser {
  public static List<AutomationTestResult> parse(ParseRequest request) throws Exception {
    TestResultParser parser;
    Boolean readFromJenkins = request.getConfiguration().getReadFromJenkins();
    if (Boolean.FALSE.equals(readFromJenkins)) {
      LoggerUtils.formatInfo(request.getListener().getLogger(), "Read test results from jenkins.");
      //read result from testResult action from Jenkins
      parser = new PublishResultParser();
    } else {
      //scan with configured pattern or scan all
      if (!StringUtils.isBlank(request.getConfiguration().getResultPattern())) {
        parser = new PatternScanParser();
      } else {
        parser = new AutoScanParser();
      }
    }
    return parser.parse(request);
  }
}
