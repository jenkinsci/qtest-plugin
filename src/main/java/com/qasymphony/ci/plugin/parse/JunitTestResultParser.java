package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author trongle
 * @version 11/2/2015 9:13 AM trongle $
 * @since 1.0
 */
public class JunitTestResultParser {
  private static final Logger LOG = Logger.getLogger(JunitTestResultParser.class.getName());

  private static final String JUNIT_PREFIX = "TEST-*";
  private static final String JUNIT_SUFFIX = "/*.xml";

  /**
   * Parse junit result
   *
   * @param build
   * @param launcher
   * @param listener
   * @return
   * @throws Exception
   */
  public static List<AutomationTestResult> parse(AbstractBuild build, Launcher launcher, BuildListener listener, Configuration configuration)
    throws Exception {
    MavenJunitParse mavenJunitParse = new MavenJunitParse(build, launcher, listener);
    if (!StringUtils.isBlank(configuration.getResultPattern())) {
      LoggerUtils.formatInfo(listener.getLogger(), "Scan with test result location: %s", configuration.getResultPattern());
      //if configured with result location pattern
      return mavenJunitParse.parse(configuration.getResultPattern());
    }
    LoggerUtils.formatInfo(listener.getLogger(), "Auto scan JUnit test results files.");

    //otherwise auto scan test result
    String basedDir = build.getWorkspace().toURI().getPath();
    List<String> resultFolders = scanJunitTestResultFolder(basedDir);
    LOG.info("Scanning junit test result in dir:" + basedDir);
    LOG.info(String.format("Found: %s dirs, %s", resultFolders.size(), resultFolders));

    Boolean isMavenProject = build.getProject().getClass().getName().toLowerCase().contains("maven");
    List<AutomationTestResult> result = new LinkedList<>();

    if (isMavenProject && resultFolders.size() <= 1) {
      //if pom file is located at workspace, we do not scan to detect junit result
      FileSet fs = Util.createFileSet(new File(basedDir), MavenJunitParse.TEST_RESULT_LOCATIONS);
      DirectoryScanner ds = fs.getDirectoryScanner();
      if (ds.getIncludedFiles().length > 0)
        return mavenJunitParse.parse();
    } else if (isMavenProject) {
      //if is maven project, we only scan surefire report folder
      for (String res : resultFolders) {
        if (res.contains(MavenJunitParse.SUREFIRE_REPORT)) {
          try {
            result.addAll(mavenJunitParse.parse(res + JUNIT_SUFFIX));
          } catch (Exception e) {
            LoggerUtils.formatWarn(listener.getLogger(), "Try to scan test result in: %s, error: %s", res, e.getMessage());
          }
        }
      }
      return result;
    }

    for (String res : resultFolders) {
      try {
        result.addAll(mavenJunitParse.parse(res + JUNIT_SUFFIX));
      } catch (Exception e) {
        LoggerUtils.formatWarn(listener.getLogger(), "Try to scan test result in: %s, error: %s", res, e.getMessage());
      }
    }
    return result;
  }

  /**
   * Scan junit test result folder
   *
   * @param basedDir
   * @return
   */
  public static List<String> scanJunitTestResultFolder(String basedDir) {
    File currentBasedDir = new File(basedDir);
    FileSet fs = Util.createFileSet(new File(basedDir), JUNIT_PREFIX);
    DirectoryScanner ds = fs.getDirectoryScanner();
    List<String> resultFolders = new ArrayList<>();
    //if based dir match junit file, we add based dir
    if (ds.getIncludedFiles().length > 0)
      resultFolders.add("");

    for (String notIncludedDirName : ds.getNotIncludedDirectories()) {
      if (!StringUtils.isEmpty(notIncludedDirName)) {
        File dirToScan = new File(currentBasedDir.getPath(), notIncludedDirName);
        FileSet subFileSet = Util.createFileSet(dirToScan, JUNIT_PREFIX);
        DirectoryScanner subDirScanner = subFileSet.getDirectoryScanner();
        if (subDirScanner.getIncludedFiles().length > 0) {
          resultFolders.add(notIncludedDirName);
        }
      }
    }
    return resultFolders;
  }
}
