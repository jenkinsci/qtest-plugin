package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Channel;
import hudson.tasks.junit.JUnitParser;
import hudson.tasks.junit.TestResult;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author anpham
 */
public class MavenJunitParser implements TestResultParser {
  private static final Logger LOG = Logger.getLogger(MavenJunitParser.class.getName());
  public static final String SUREFIRE_REPORT = "surefire-reports";
  public static final String TEST_RESULT_LOCATIONS = "target/surefire-reports/*.xml";

  public List<AutomationTestResult> parse(ParseRequest request, String testResultLocation) throws Exception {
    JUnitParser jUnitParser = new JUnitParser(true);
    List<TestResult> testResults = new ArrayList<TestResult>();
    AbstractBuild build = request.getBuild();
    Launcher launcher = request.getLauncher();
    BuildListener listener = request.getListener();
    testResults.add(jUnitParser.parseResult(testResultLocation, build, build.getWorkspace(), launcher, listener));

    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTimeInMillis(build.getStartTimeInMillis());

    return CommonParsingUtils.toAutomationTestResults(testResults, gregorianCalendar.getTime(), build.getTime());
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  @Override
  public List<AutomationTestResult> parse(ParseRequest request)
    throws Exception {
    Configuration configuration = request.getConfiguration();
    BuildListener listener = request.getListener();
    AbstractBuild build = request.getBuild();
    if (!StringUtils.isBlank(configuration.getResultPattern())) {
      LoggerUtils.formatInfo(listener.getLogger(), "Scan with test result location: %s", configuration.getResultPattern());
      //if configured with result location pattern
      return parse(request, configuration.getResultPattern());
    }
    LoggerUtils.formatInfo(listener.getLogger(), "Auto scan JUnit test results files.");

    //otherwise auto scan test result
    String basedDir = build.getWorkspace().toURI().getPath();
    List<String> resultFolders = CommonParsingUtils.scanJunitTestResultFolder(basedDir);
    LOG.info("Scanning junit test result in dir:" + basedDir);
    LOG.info(String.format("Found: %s dirs, %s", resultFolders.size(), resultFolders));

    Boolean isMavenProject = build.getProject().getClass().getName().toLowerCase().contains("maven");
    List<AutomationTestResult> result = new LinkedList<>();

    if (isMavenProject && resultFolders.size() <= 1) {
      //if pom file is located at workspace, we do not scan to detect junit result
      FileSet fs = Util.createFileSet(new File(basedDir), MavenJunitParser.TEST_RESULT_LOCATIONS);
      DirectoryScanner ds = fs.getDirectoryScanner();
      if (ds.getIncludedFiles().length > 0)
        return parse(request, TEST_RESULT_LOCATIONS);
    } else if (isMavenProject) {
      //if is maven project, we only scan surefire report folder
      for (String res : resultFolders) {
        if (res.contains(MavenJunitParser.SUREFIRE_REPORT)) {
          try {
            result.addAll(parse(request, res + CommonParsingUtils.JUNIT_SUFFIX));
          } catch (Exception e) {
            LoggerUtils.formatWarn(listener.getLogger(), "Try to scan test result in: %s, error: %s", res, e.getMessage());
          }
        }
      }
      return result;
    }

    for (String res : resultFolders) {
      try {
        result.addAll(parse(request, res + CommonParsingUtils.JUNIT_SUFFIX));
      } catch (Exception e) {
        LoggerUtils.formatWarn(listener.getLogger(), "Try to scan test result in: %s, error: %s", res, e.getMessage());
      }
    }
    return result;
  }
}
