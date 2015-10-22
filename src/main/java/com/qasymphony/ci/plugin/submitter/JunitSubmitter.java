package com.qasymphony.ci.plugin.submitter;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public interface JunitSubmitter {
  JunitSubmitterResult submit(JunitSubmitterRequest junitSubmitterRequest);

  void storeSubmitterResult();
}
