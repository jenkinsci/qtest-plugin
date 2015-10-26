package com.qasymphony.ci.plugin.submitter;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public interface JunitSubmitter {
  JunitSubmitterResult submit(JunitSubmitterRequest junitSubmitterRequest);

  void storeSubmitterResult();
  
  public void push(String testResultLocations, Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException;
}
