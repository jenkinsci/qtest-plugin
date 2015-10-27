package com.qasymphony.ci.plugin.submitter;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Run;

import java.util.Map;

import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.utils.ResponseEntity;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public interface JunitSubmitter {
  JunitSubmitterResult submit(JunitSubmitterRequest junitSubmitterRequest);

  void storeSubmitterResult();
  
  public ResponseEntity push(String testResultLocations, Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener, Configuration configuration, Map<String, String> headers) throws Exception;
}
