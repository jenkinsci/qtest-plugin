package com.qasymphony.ci.plugin.parse;

import com.qasymphony.ci.plugin.model.Configuration;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * @author trongle
 * @version 12/14/2015 10:10 AM trongle $
 * @since 1.0
 */
public class ParseRequest {

  private Run<?, ?> build;

  private Launcher launcher;
  private TaskListener listener;
  private Boolean isMavenProject;

  public FilePath getWorkSpace() {
    return workSpace;
  }

  public ParseRequest setWorkSpace(FilePath workSpace) {
    this.workSpace = workSpace;
    return this;
  }

  private FilePath workSpace;

  public Boolean getOverwriteExistingTestSteps() {
    return overwriteExistingTestSteps;
  }

  public ParseRequest setOverwriteExistingTestSteps(Boolean overwriteExistingTestSteps) {
    this.overwriteExistingTestSteps = overwriteExistingTestSteps;
    return this;
  }

  private Boolean overwriteExistingTestSteps;

  public Boolean getCreateEachMethodAsTestCase() {
    return createEachMethodAsTestCase;
  }

  public ParseRequest setCreateEachMethodAsTestCase(Boolean createEachMethodAsTestCase) {
    this.createEachMethodAsTestCase = createEachMethodAsTestCase;
    return this;
  }

  public Boolean getUtilizeTestResultFromCITool() {
    return utilizeTestResultFromCITool;
  }

  public ParseRequest setUtilizeTestResultFromCITool(Boolean utilizeTestResultFromCITool) {
    this.utilizeTestResultFromCITool = utilizeTestResultFromCITool;
    return this;
  }

  public String getParseTestResultPattern() {
    return parseTestResultPattern;
  }

  public ParseRequest setParseTestResultPattern(String parseTestResultPattern) {
    this.parseTestResultPattern = parseTestResultPattern;
    return this;
  }

  private Boolean createEachMethodAsTestCase;
  private Boolean utilizeTestResultFromCITool;
  private String parseTestResultPattern;

  public Run<?, ?> getBuild() {
    return build;
  }

  public ParseRequest setBuild(Run<?, ?> build) {
    this.build = build;
    if (build instanceof AbstractBuild) {
      isMavenProject = null == build ? false : ((AbstractBuild)build).getProject().getClass().getName().toLowerCase().contains("maven");
    }
    return this;
  }



  public Launcher getLauncher() {
    return launcher;
  }

  public ParseRequest setLauncher(Launcher launcher) {
    this.launcher = launcher;
    return this;
  }

  public TaskListener getListener() {
    return listener;
  }

  public ParseRequest setListener(TaskListener listener) {
    this.listener = listener;
    return this;
  }

  public Boolean isMavenProject() {
    return isMavenProject;
  }
  
}
