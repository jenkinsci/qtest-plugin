package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.parse.CommonParsingUtils;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.parse.ParseRequest;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author trongle
 * @version 11/2/2015 11:19 AM trongle $
 * @since 1.0
 */
public class ParseJunitWithNunitTests extends TestAbstracts {

  public static final class ParseJunitFromNunitProject extends Builder implements Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean perform(AbstractBuild<?, ?> build,
      Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
      try {
        File currentBasedDir = new File(build.getWorkspace().toURI());
        List<String> matchDirs = CommonParsingUtils.scanJunitTestResultFolder(currentBasedDir.getPath());
        long current = System.currentTimeMillis();
        for (String dir : matchDirs) {
          File testFolder = new File(currentBasedDir.getPath(), dir);
          testFolder.setLastModified(current);
          for (File file : testFolder.listFiles()) {
            file.setLastModified(current);
          }
        }
        automationTestResultList = JunitTestResultParser.parse(new ParseRequest()
        .setBuild(build)
        .setListener(listener)
        .setLauncher(launcher)
        .setConfiguration(Configuration.newInstance()));
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }
  }

  private FreeStyleProject project;
  private static List<AutomationTestResult> automationTestResultList;

  @Before public void setUp() throws Exception {
  }

  @LocalData
  @Test public void testJunitFromNUnitResultProject()
    throws InterruptedException, ExecutionException, TimeoutException, IOException {

    project = j.createFreeStyleProject("nunit-junit");
    automationTestResultList = null;
    project.getBuildersList().add(new ParseJunitFromNunitProject());
    FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
    assertNotNull("Build is: ", build);
    assertEquals("", 2, automationTestResultList.size());
  }
}
