package com.qasymphony.ci.plugin.store;

import hudson.Functions;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;

import java.io.File;
import java.net.URLConnection;

/**
 * @author trongle
 * @version 11/2/2015 11:20 AM trongle $
 * @since 1.0
 */
public class TestAbstracts {

  @Rule public JenkinsRule j = new JenkinsRule() {
    private boolean origDefaultUseCache = true;

    @Override
    public void before() throws Throwable {
      if (Functions.isWindows()) {
        URLConnection aConnection = new File(".").toURI().toURL().openConnection();
        origDefaultUseCache = aConnection.getDefaultUseCaches();
        aConnection.setDefaultUseCaches(false);
      }
      super.before();
    }

    @Override
    public void after() throws Exception {
      super.after();
      if (TestEnvironment.get() != null)
        try {
          TestEnvironment.get().dispose();
        } catch (Exception e) {
          e.printStackTrace();
        }
      if (Functions.isWindows()) {
        URLConnection aConnection = new File(".").toURI().toURL().openConnection();
        aConnection.setDefaultUseCaches(origDefaultUseCache);
      }
    }
  };
}
