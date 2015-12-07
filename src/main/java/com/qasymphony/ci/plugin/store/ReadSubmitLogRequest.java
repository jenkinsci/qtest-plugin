package com.qasymphony.ci.plugin.store;

import hudson.model.AbstractProject;

/**
 * @author trongle
 * @version 12/4/2015 5:06 PM trongle $
 * @since 1.0
 */
public class ReadSubmitLogRequest {
  private AbstractProject project;
  private int start;
  private int size;

  public AbstractProject getProject() {
    return project;
  }

  public ReadSubmitLogRequest setProject(AbstractProject project) {
    this.project = project;
    return this;
  }

  public int getStart() {
    return start;
  }

  public ReadSubmitLogRequest setStart(int start) {
    this.start = start;
    return this;
  }

  public int getSize() {
    return size;
  }

  public ReadSubmitLogRequest setSize(int size) {
    this.size = size;
    return this;
  }

  public int getCurrentBuildNumber() {
    return this.project == null ? 0 : Math.max(this.project.getNextBuildNumber() - 1, 0);
  }
}
