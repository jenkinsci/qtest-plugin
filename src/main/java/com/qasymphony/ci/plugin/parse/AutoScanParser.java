package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.Constants;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Util;
import hudson.model.AbstractBuild;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * scan test result with auto detect JUnit test result location
 *
 * @author trongle
 * @version 11/2/2015 9:13 AM trongle $
 * @since 1.0
 */
public class AutoScanParser extends PatternScanParser {
  private static final Logger LOG = Logger.getLogger(AutoScanParser.class.getName());
  public static final String SUREFIRE_REPORT = "surefire-reports";
  public static final String TEST_RESULT_LOCATIONS = "target/surefire-reports/*.xml";

  /**
   * @param request
   * @return
   * @throws Exception
   */
  @Override
  public List<AutomationTestResult> parse(ParseRequest request)
    throws Exception {
    PrintStream logger = request.getListener().getLogger();
    LoggerUtils.formatInfo(logger, "Auto scan JUnit test results files.");

    AbstractBuild build = request.getBuild();

    //otherwise auto scan test result
    String basedDir = build.getWorkspace().toURI().getPath();
    List<String> resultFolders = CommonParsingUtils.scanJunitTestResultFolder(basedDir);
    LOG.info("Scanning junit test result in dir:" + basedDir + String.format(".Found: %s dirs, %s", resultFolders.size(), resultFolders));

    List<AutomationTestResult> result = new LinkedList<>();

    if (request.isMavenProject() && resultFolders.size() <= 1) {
      //if pom file is located at workspace, we do not scan to detect junit result
      FileSet fs = Util.createFileSet(new File(basedDir), AutoScanParser.TEST_RESULT_LOCATIONS);
      DirectoryScanner ds = fs.getDirectoryScanner();
      if (ds.getIncludedFiles().length > 0)
        return parse(request, TEST_RESULT_LOCATIONS);
    } else if (request.isMavenProject()) {
      //if is maven project, we only scan surefire report folder
      for (String res : resultFolders) {
        if (res.contains(AutoScanParser.SUREFIRE_REPORT)) {
          try {
            result.addAll(parse(request, res + Constants.JUNIT_SUFFIX));
          } catch (Exception e) {
            LoggerUtils.formatWarn(logger, "Try to scan test result in: %s, error: %s", res, e.getMessage());
          }
        }
      }
      return result;
    }

    for (String res : resultFolders) {
      try {
        result.addAll(parse(request, res + Constants.JUNIT_SUFFIX));
      } catch (Exception e) {
        LoggerUtils.formatWarn(logger, "Try to scan test result in: %s, error: %s", res, e.getMessage());
      }
    }
    return result;
  }
}
