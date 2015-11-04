/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author anpham
 */
public class PushingResultAction extends Notifier {
  private static final Logger LOG = Logger.getLogger(PushingResultAction.class.getName());
  private Configuration configuration;

  public PushingResultAction(Configuration configuration) {
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    DescriptorImpl descriptor = (DescriptorImpl) super.getDescriptor();
    return descriptor;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
    throws InterruptedException, IOException {
    PrintStream logger = listener.getLogger();
    showInfo(logger);
    if (!validateConfig(configuration)) {
      formatWarn(logger, "Invalid configuration to qTest, reject submit test result.");
      return true;
    }
    checkProjectNameChanged(build, logger);

    JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
    JunitSubmitterResult result = submitTestResult(build, launcher, listener, logger, junitSubmitter);
    if (null == result) {
      //if have no test result, we do not break build flow
      return true;
    }
    saveConfiguration(build, result, logger);
    storeResult(build, junitSubmitter, result, logger);
    return true;
  }

  private void showInfo(PrintStream logger) {
    logger.println("");
    formatInfo(logger, "------------------------------------------------------------------------");
    formatInfo(logger, ResourceBundle.DISPLAY_NAME);
    formatInfo(logger, "------------------------------------------------------------------------");
    formatInfo(logger, "Submit Junit to qTest at:%s", configuration.getUrl());
    formatInfo(logger, "With project: %s, %s.", configuration.getProjectId(), configuration.getProjectName());
    formatInfo(logger, "With release: %s, %s.", configuration.getReleaseId(), configuration.getReleaseName());
    if (configuration.getEnvironmentId() > 0) {
      formatInfo(logger, "With environment: %s, %s.", configuration.getEnvironmentId(), configuration.getEnvironmentName());
    } else {
      formatInfo(logger, "With no environment.");
    }
    logger.println("");
  }

  private Boolean validateConfig(Configuration configuration) {
    return configuration != null &&
      !StringUtils.isEmpty(configuration.getUrl()) &&
      !StringUtils.isEmpty(configuration.getAppSecretKey()) &&
      configuration.getProjectId() > 0 &&
      configuration.getReleaseId() > 0;
  }

  private void checkProjectNameChanged(AbstractBuild build, PrintStream logger) {
    String currentJenkinProjectName = build.getProject().getName();
    if (!configuration.getJenkinsProjectName().equals(currentJenkinProjectName)) {
      formatInfo(logger, "Current job name [%s] is changed with previous configuration.", currentJenkinProjectName);
      configuration.setJenkinsProjectName(currentJenkinProjectName);
      ConfigService.saveConfiguration(configuration);
    }
  }

  private JunitSubmitterResult submitTestResult(AbstractBuild build, Launcher launcher, BuildListener listener, PrintStream logger, JunitSubmitter junitSubmitter) {
    List<AutomationTestResult> automationTestResults;
    try {
      automationTestResults = JunitTestResultParser.parse(build, launcher, listener);
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
      automationTestResults = Collections.emptyList();
    }

    if (automationTestResults.isEmpty()) {
      formatWarn(logger, "No junit test result found.");
      return null;
    }
    formatInfo(logger, "Junit test result found: %s", automationTestResults.size());

    JunitSubmitterResult result = null;
    formatInfo(logger, "Begin submit test result to qTest,at: " + JsonUtils.getCurrentDateString());
    try {
      result = junitSubmitter.submit(
        new JunitSubmitterRequest()
          .setConfiguration(configuration)
          .setTestResults(automationTestResults)
          .setBuildNumber(build.getNumber() + "")
          .setBuildPath(build.getUrl()));
    } catch (SubmittedException e) {
      formatError(logger, "Cannot submit test result to qTest:" + e.getMessage());
      e.printStackTrace(logger);
    } finally {
      if (null == result) {
        result = new JunitSubmitterResult()
          .setTestSuiteId(null)
          .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
          .setNumberOfTestResult(automationTestResults.size())
          .setNumberOfTestRun(0);
      }
      formatInfo(logger, "Result after submit: testRuns=%s, testSuiteId:%s, testSuiteName:%s",
        result.getNumberOfTestRun(), result.getTestSuiteId(), result.getTestSuiteName());
      formatInfo(logger, "End submit test result to qTest at: %s", JsonUtils.getCurrentDateString());
    }
    return result;
  }

  private void saveConfiguration(AbstractBuild build, JunitSubmitterResult result, PrintStream logger) {
    //set testSuite id created from qTest
    configuration.setTestSuiteId(null == result.getTestSuiteId() ? configuration.getTestSuiteId() : result.getTestSuiteId());
    try {
      build.getProject().save();
      formatInfo(logger, "Save test suite to configuration, testSuiteId=%s", configuration.getTestSuiteId());
    } catch (IOException e) {
      formatError(logger, "Cannot save test suite to configuration of project:%s", e.getMessage());
      e.printStackTrace(logger);
    }
  }

  private void storeResult(AbstractBuild build, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
    logger.println("");
    formatInfo(logger, "Begin store submitted result to workspace.");
    try {
      junitSubmitter.storeSubmittedResult(build, result);
    } catch (Exception e) {
      formatError(logger, "Cannot store submitted result." + e.getMessage());
      e.printStackTrace(logger);
    }
    formatInfo(logger, "End store submitted result.");
    logger.println("");
  }

  private void formatInfo(PrintStream logger, String msg, Object... args) {
    format(logger, "INFO", msg, args);
  }

  private void formatError(PrintStream logger, String msg, Object... args) {

    format(logger, "ERROR", msg, args);
  }

  private void formatWarn(PrintStream logger, String msg, Object... args) {
    format(logger, "WARN", msg, args);
  }

  private void format(PrintStream logger, String level, String msg, Object... args) {
    logger.println(String.format("[%s] %s", level, String.format(msg, args)));
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      super(PushingResultAction.class);
      load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return ResourceBundle.DISPLAY_NAME;
    }

    @Override
    public String getHelpFile() {
      return "/plugin/jqtest/help/main.html";
    }

    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
      Configuration configuration = req.bindParameters(Configuration.class, "config.");
      configuration.setJenkinsServerUrl(getServerUrl(req));
      configuration.setJenkinsProjectName(req.getParameter("name"));
      Setting setting = ConfigService.saveConfiguration(configuration);
      if (null != setting) {
        configuration.setModuleId(setting.getModuleId());
        configuration.setId(setting.getId());
      }
      return new PushingResultAction(configuration);
    }

    private String getServerUrl(StaplerRequest request) {
      Boolean isDefaultPort = request.getServerPort() == 443 || request.getServerPort() == 80;
      return String.format("%s://%s%s%s%s", request.getScheme(), request.getServerName(),
        isDefaultPort ? "" : ":", request.getServerPort(), request.getContextPath());
    }

    public FormValidation doCheckUrl(@QueryParameter String value)
      throws IOException, ServletException {
      try {
        new URL(value);
        Boolean isQtestUrl = ConfigService.validateQtestUrl(value);
        if (isQtestUrl) {
          return FormValidation.ok();
        } else {
          return FormValidation.error("Please set a valid qTest URL");
        }
      } catch (Exception e) {
        return FormValidation.error("Please set a valid qTest URL");
      }
    }

    public FormValidation doCheckAppSecretKey(@QueryParameter String value)
      throws IOException, ServletException {
      StaplerRequest request = Stapler.getCurrentRequest();
      if (StringUtils.isEmpty(value))
        return FormValidation.error("Please set a API key");
      return FormValidation.ok();
    }

    public FormValidation doCheckProjectName(@QueryParameter String value)
      throws IOException, ServletException {
      if (value.length() <= 0)
        return FormValidation.error("Please select a project.");
      return FormValidation.ok();
    }

    public FormValidation doCheckReleaseName(@QueryParameter String value)
      throws IOException, ServletException {
      if (value.length() <= 0)
        return FormValidation.error("Please select a release.");
      return FormValidation.ok();
    }

    public FormValidation doCheckEnvironment(@QueryParameter String value)
      throws IOException, ServletException {
      return FormValidation.ok();
    }

    /**
     * @param qTestUrl
     * @param apiKey
     * @return
     */
    @JavaScriptMethod
    public JSONObject getProjects(String qTestUrl, String apiKey) {
      JSONObject res = new JSONObject();
      //get project from qTest
      Object projects = ConfigService.getProjects(qTestUrl, apiKey);
      res.put("projects", null == projects ? "" : JSONArray.fromObject(projects));
      return res;
    }

    /**
     * @param qTestUrl
     * @param apiKey
     * @param projectId
     * @return
     */
    @JavaScriptMethod
    public JSONObject getProjectData(final String qTestUrl, final String apiKey, final Long projectId, final String jenkinsProjectName) {
      final JSONObject res = new JSONObject();
      StaplerRequest request = Stapler.getCurrentRequest();
      final String jenkinsServerName = getServerUrl(request);

      final CountDownLatch countDownLatch = new CountDownLatch(3);
      ExecutorService fixedPool = Executors.newFixedThreadPool(3);

      Callable<Object> caGetSetting = new Callable<Object>() {
        @Override public Object call() throws Exception {
          try {
            //get saved setting from qtest
            Object setting = ConfigService.getConfiguration(qTestUrl, apiKey, jenkinsServerName,
              HttpClientUtils.encode(jenkinsProjectName), projectId);
            res.put("setting", null == setting ? "" : JSONObject.fromObject(setting));
            return setting;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      Callable<Object> caGetReleases = new Callable<Object>() {
        @Override public Object call() throws Exception {
          try {
            Object releases = ConfigService.getReleases(qTestUrl, apiKey, projectId);
            res.put("releases", null == releases ? "" : JSONArray.fromObject(releases));
            return releases;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      Callable<Object> caGetEnvs = new Callable<Object>() {
        @Override public Object call() throws Exception {
          try {
            Object environments = ConfigService.getEnvironments(qTestUrl, apiKey, projectId);
            res.put("environments", null == environments ? "" : environments);
            return environments;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      fixedPool.submit(caGetSetting);
      fixedPool.submit(caGetReleases);
      fixedPool.submit(caGetEnvs);
      try {
        countDownLatch.await();
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING, e.getMessage());
      } finally {
        fixedPool.shutdownNow();
        return res;
      }
    }
  }
}
