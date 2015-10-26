package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.model.Configuration;

/**
 * @author trongle
 * @version 10/21/2015 2:37 PM trongle $
 * @since 1.0
 */
public class JunitSubmitterRequest {
  private Configuration configuration;

  public Configuration getConfiguration() {
    return configuration;
  }

  public JunitSubmitterRequest setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }

  public JunitSubmitterRequest withSetting(Configuration configuration) {
    return setConfiguration(configuration);
  }

}