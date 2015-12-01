package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.exception.SaveSettingException;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
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
  private static final String HR_TEXT = "------------------------------------------------------------------------";
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
    return (DescriptorImpl) super.getDescriptor();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
    throws InterruptedException, IOException {
    PrintStream logger = listener.getLogger();
    JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
    if (Result.ABORTED.equals(build.getResult())) {
      formatWarn(logger, "Abort build action.");
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_CANCELED);
      return true;
    }
    showInfo(logger);
    if (!validateConfig(configuration)) {
      formatWarn(logger, "Invalid configuration to qTest, reject submit test result.");
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_FAILED);
      return true;
    }
    checkProjectNameChanged(build, logger);

    JunitSubmitterResult result = submitTestResult(build, launcher, listener, logger, junitSubmitter);
    if (null == result) {
      //if have no test result, we do not break build flow
      return true;
    }
    saveConfiguration(build, result, logger);
    storeResult(build, junitSubmitter, result, logger);
    return true;
  }

  private Boolean storeWhenNotSuccess(JunitSubmitter junitSubmitter, AbstractBuild build, PrintStream logger, String status) {
    try {
      junitSubmitter.storeSubmittedResult(build, new JunitSubmitterResult()
        .setNumberOfTestLog(0)
        .setTestSuiteName("")
        .setNumberOfTestResult(0)
        .setTestSuiteId(null)
        .setSubmittedStatus(status));
    } catch (StoreResultException e) {
      formatError(logger, e.getMessage());
      e.printStackTrace(logger);
    } finally {
      return true;
    }
  }

  private void showInfo(PrintStream logger) {
    formatInfo(logger, "");
    formatInfo(logger, HR_TEXT);
    formatInfo(logger, ResourceBundle.DISPLAY_NAME);
    formatInfo(logger, HR_TEXT);
    formatInfo(logger, "Submit Junit test result to qTest at:%s", configuration.getUrl());
    formatInfo(logger, "With project: %s (id=%s).", configuration.getProjectName(), configuration.getProjectId());
    formatInfo(logger, "With release: %s (id=%s).", configuration.getReleaseName(), configuration.getReleaseId());
    if (configuration.getEnvironmentId() > 0) {
      formatInfo(logger, "With environment: %s (id=%s).", configuration.getEnvironmentName(), configuration.getEnvironmentId());
    } else {
      formatInfo(logger, "With no environment.");
    }
    formatInfo(logger, "");
  }

  private Boolean validateConfig(Configuration configuration) {
    return configuration != null &&
      !StringUtils.isEmpty(configuration.getUrl()) &&
      !StringUtils.isEmpty(configuration.getAppSecretKey()) &&
      configuration.getProjectId() > 0 &&
      configuration.getReleaseId() > 0;
  }

  private Setting checkProjectNameChanged(AbstractBuild build, PrintStream logger) {
    String currentJenkinProjectName = build.getProject().getName();
    if (!configuration.getJenkinsProjectName().equals(currentJenkinProjectName)) {
      formatInfo(logger, "Current job name [%s] is changed with previous configuration, update configuration to qTest.", currentJenkinProjectName);
      configuration.setJenkinsProjectName(currentJenkinProjectName);
    }
    Setting setting = null;
    try {
      setting = ConfigService.saveConfiguration(configuration);
    } catch (SaveSettingException e) {
      formatWarn(logger, "Cannot update ci setting to qTest:" + e.getMessage());
    }
    if (null != setting) {
      configuration.setId(setting.getId());
      configuration.setModuleId(setting.getModuleId());
    }
    return setting;
  }

  private JunitSubmitterResult submitTestResult(AbstractBuild build, Launcher launcher, BuildListener listener, PrintStream logger, JunitSubmitter junitSubmitter) {
    List<AutomationTestResult> automationTestResults;
    long start = System.currentTimeMillis();
    try {
      automationTestResults = JunitTestResultParser.parse(build, launcher, listener);
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
      formatError(logger, e.getMessage());
      automationTestResults = Collections.emptyList();
    }
    if (automationTestResults.isEmpty()) {
      formatWarn(logger, "No JUnit test result found.");
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_SKIPPED);
      return null;
    }
    formatInfo(logger, HR_TEXT);
    formatInfo(logger, "JUnit test result found: %s", automationTestResults.size());
    formatInfo(logger, "Time to parse in: " + LoggerUtils.eslapedTime(start));
    formatInfo(logger, HR_TEXT);

    formatInfo(logger, "");
    JunitSubmitterResult result = null;
    formatInfo(logger, "Begin submit test result to qTest at: " + JsonUtils.getCurrentDateString());
    start = System.currentTimeMillis();
    try {
      result = junitSubmitter.submit(
        new JunitSubmitterRequest()
          .setConfiguration(configuration)
          .setTestResults(automationTestResults)
          .setBuildNumber(build.getNumber() + "")
          .setBuildPath(build.getUrl()));
    } catch (SubmittedException e) {
      formatError(logger, "Cannot submit test result to qTest:");
      formatError(logger, "   status code: " + e.getStatus());
      formatError(logger, "   error: " + e.getMessage());
    } catch (Exception e) {
      formatError(logger, "Cannot submit test result to qTest:");
      formatError(logger, "   error: " + e.getMessage());
    } finally {
      if (null == result) {
        result = new JunitSubmitterResult()
          .setTestSuiteId(null)
          .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
          .setNumberOfTestResult(automationTestResults.size())
          .setNumberOfTestLog(0);
      }

      formatInfo(logger, "Result after submit:");
      formatInfo(logger, HR_TEXT);
      if (null == result.getTestSuiteId() || result.getTestSuiteId() <= 0) {
        formatInfo(logger, "SUBMIT FAILED");
        formatInfo(logger, HR_TEXT);
      } else {
        formatInfo(logger, "SUBMIT SUCCESS");
        formatInfo(logger, HR_TEXT);
        formatInfo(logger, "   testLogs: %s", result.getNumberOfTestLog());
        formatInfo(logger, "   testSuite: name=%s, id=%s", result.getTestSuiteName(), result.getTestSuiteId());
        formatInfo(logger, "   link: %s", ConfigService.formatTestSuiteLink(configuration.getUrl(), configuration.getProjectId(), result.getTestSuiteId()));
      }
      formatInfo(logger, "   time to submit in: " + LoggerUtils.eslapedTime(start));
      formatInfo(logger, "End submit test result to qTest at: %s", JsonUtils.getCurrentDateString());
      formatInfo(logger, "");
    }
    return result;
  }

  private void saveConfiguration(AbstractBuild build, JunitSubmitterResult result, PrintStream logger) {
    //set testSuite id created from qTest
    configuration.setTestSuiteId(null == result.getTestSuiteId() ? configuration.getTestSuiteId() : result.getTestSuiteId());
    try {
      build.getProject().save();
      formatInfo(logger, "Save test suite to configuration success.");
    } catch (IOException e) {
      formatError(logger, "Cannot save test suite to configuration of project:%s", e.getMessage());
      e.printStackTrace(logger);
    }
  }

  private void storeResult(AbstractBuild build, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
    formatInfo(logger, "");
    formatInfo(logger, "Begin store submitted result to workspace.");
    try {
      junitSubmitter.storeSubmittedResult(build, result);
    } catch (Exception e) {
      formatError(logger, "Cannot store submitted result." + e.getMessage());
      e.printStackTrace(logger);
    }
    formatInfo(logger, "End store submitted result.");
    formatInfo(logger, "");
  }

  private void formatInfo(PrintStream logger, String msg, Object... args) {
    LoggerUtils.formatInfo(logger, msg, args);
  }

  private void formatError(PrintStream logger, String msg, Object... args) {
    LoggerUtils.formatError(logger, msg, args);
  }

  private void formatWarn(PrintStream logger, String msg, Object... args) {
    LoggerUtils.formatWarn(logger, msg, args);
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
      return ResourceBundle.CONFIG_HELP_FILE;
    }

    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
      Configuration configuration = req.bindParameters(Configuration.class, "config.");
      configuration.setJenkinsServerUrl(getServerUrl(req));
      configuration.setJenkinsProjectName(req.getParameter("name"));

      configuration = ConfigService.validateConfiguration(configuration, formData);

      Setting setting = null;
      try {
        setting = ConfigService.saveConfiguration(configuration);
      } catch (SaveSettingException e) {
        LOG.log(Level.WARNING, e.getMessage());
      }
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

    public FormValidation doCheckUrl(@QueryParameter String value, @AncestorInPath AbstractProject project)
      throws IOException, ServletException {
      try {
        new URL(value);
        Boolean isQtestUrl = ConfigService.validateQtestUrl(value);
        if (isQtestUrl) {
          DescribableList<Publisher, Descriptor<Publisher>> publishers = project.getPublishersList();
          PushingResultAction notifier = (PushingResultAction) publishers.get(this);
          if (null != notifier && notifier.getConfiguration() != null) {
            //set url to can get url when validate apiKey
            notifier.getConfiguration().setUrl(value);
          }
          return FormValidation.ok();
        } else {
          return FormValidation.error(ResourceBundle.MSG_INVALID_URL);
        }
      } catch (Exception e) {
        return FormValidation.error(ResourceBundle.MSG_INVALID_URL);
      }
    }

    public FormValidation doCheckAppSecretKey(@QueryParameter String value, @AncestorInPath AbstractProject project)
      throws IOException, ServletException {
      if (StringUtils.isEmpty(value))
        return FormValidation.error(ResourceBundle.MSG_INVALID_API_KEY);
      DescribableList<Publisher, Descriptor<Publisher>> publishers = project.getPublishersList();
      PushingResultAction notifier = (PushingResultAction) publishers.get(this);
      if (null != notifier && notifier.getConfiguration() != null) {
        if (!ConfigService.validateApiKey(notifier.getConfiguration().getUrl(), value))
          return FormValidation.error(ResourceBundle.MSG_INVALID_API_KEY);
      }
      return FormValidation.ok();
    }

    public FormValidation doCheckProjectName(@QueryParameter String value)
      throws IOException, ServletException {
      if (value.length() <= 0)
        return FormValidation.error(ResourceBundle.MSG_INVALID_PROJECT);
      return FormValidation.ok();
    }

    public FormValidation doCheckReleaseName(@QueryParameter String value)
      throws IOException, ServletException {
      if (value.length() <= 0)
        return FormValidation.error(ResourceBundle.MSG_INVALID_RELEASE);
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
      final String accessToken = OauthProvider.getAccessToken(qTestUrl, apiKey);

      Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
      if (null == project) {
        //if project not found, we return empty data
        res.put("setting", "");
        res.put("releases", "");
        res.put("environments", "");
        return res;
      }

      final CountDownLatch countDownLatch = new CountDownLatch(3);
      ExecutorService fixedPool = Executors.newFixedThreadPool(3);

      Callable<Object> caGetSetting = new Callable<Object>() {
        @Override public Object call() throws Exception {
          try {
            //get saved setting from qtest
            Object setting = ConfigService.getConfiguration(new Setting().setJenkinsServer(jenkinsServerName)
              .setJenkinsProjectName(jenkinsProjectName)
              .setProjectId(projectId), qTestUrl, accessToken);
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
            Object releases = ConfigService.getReleases(qTestUrl, accessToken, projectId);
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
            Object environments = ConfigService.getEnvironments(qTestUrl, accessToken, projectId);
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
