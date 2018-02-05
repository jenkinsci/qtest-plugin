package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.parse.ParseRequest;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
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
      LoggerUtils.formatWarn(logger, "Abort build action.");
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_CANCELED);
      return true;
    }
    showInfo(logger);
    if (!validateConfig(configuration)) {
      LoggerUtils.formatWarn(logger, "Invalid configuration to qTest, reject submit test results.");
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_FAILED);
      return true;
    }
    if (null == checkProjectNameChanged(build, listener)) {
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_CANCELED);
      return true;
    }
    List<AutomationTestResult> automationTestResults = readTestResults(build, launcher, listener, logger, junitSubmitter);
    if (automationTestResults.isEmpty())
      return true;
    JunitSubmitterResult result = submitTestResult(build, listener, junitSubmitter, automationTestResults);
    if (null == result) {
      //if have no test result, we do not break build flow
      return true;
    }
    saveConfiguration(build, result, logger);
    storeResult(build, junitSubmitter, result, logger);
    LoggerUtils.formatHR(logger);
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
      LoggerUtils.formatError(logger, e.getMessage());
      e.printStackTrace(logger);
    }
    return true;
  }

  private void showInfo(PrintStream logger) {
    LoggerUtils.formatInfo(logger, "");
    LoggerUtils.formatHR(logger);
    LoggerUtils.formatInfo(logger, ResourceBundle.DISPLAY_NAME);
    LoggerUtils.formatInfo(logger, String.format("Build Version: %s", ConfigService.getBuildVersion()));
    LoggerUtils.formatHR(logger);
    LoggerUtils.formatInfo(logger, "Submit Junit test result to qTest at:%s (cid:%s)", configuration.getUrl(), configuration.getId());
    LoggerUtils.formatInfo(logger, "With project: %s (id=%s).", configuration.getProjectName(), configuration.getProjectId());
    if (!configuration.isSubmitToContainer()) {
      LoggerUtils.formatInfo(logger, "With release: %s (id=%s).", configuration.getReleaseName(), configuration.getReleaseId());
    } else {
      Long nodeId = 0L;
      String nodeType = "N/A";
      JSONObject json = configuration.getContainerJSONObject();
      if (null != json) {
        JSONArray containerPath = json.optJSONArray("containerPath");
        if (null != containerPath && 0 < containerPath.size()) {
          nodeId = containerPath.getJSONObject(containerPath.size() - 1).optLong("nodeId", 0L);
          nodeType = containerPath.getJSONObject(containerPath.size() - 1).optString("nodeType", "");
        }
      }

      LoggerUtils.formatInfo(logger, "With container: %s (id=%s, type=%s).",
              json.getJSONObject("selectedContainer").getString("name"),
              nodeId, nodeType);
    }
    if (configuration.getEnvironmentId() > 0) {
      LoggerUtils.formatInfo(logger, "With environment: %s (id=%s).", configuration.getEnvironmentName(), configuration.getEnvironmentId());
    } else {
      LoggerUtils.formatInfo(logger, "With no environment.");
    }
    LoggerUtils.formatInfo(logger, "");
  }

  private Boolean validateConfig(Configuration configuration) {
    if (null == configuration
            || StringUtils.isEmpty(configuration.getUrl())
            || StringUtils.isEmpty(configuration.getAppSecretKey())
            || 0 >= configuration.getProjectId()) {
      return false;
    }
    if (!configuration.isSubmitToContainer()) {
      return configuration.getReleaseId() > 0;
    } else {
      return null != configuration.getContainerJSONObject();
    }
  }

  private Setting checkProjectNameChanged(AbstractBuild build, BuildListener listener) {
    String currentJenkinsProjectName = build.getProject().getName();
    PrintStream logger = listener.getLogger();
    if (!configuration.getJenkinsProjectName().equals(currentJenkinsProjectName)) {
      LoggerUtils.formatInfo(logger, "Current job name [%s] is changed with previous configuration, update configuration to qTest.", currentJenkinsProjectName);
      configuration.setJenkinsProjectName(currentJenkinsProjectName);
    }
    Setting setting = null;
    try {
      setting = ConfigService.saveConfiguration(configuration);
    } catch (Exception e) {
      LoggerUtils.formatWarn(logger, "Cannot update ci setting to qTest:");
      LoggerUtils.formatWarn(logger, "  Error: %s", e.getMessage());
      e.printStackTrace(logger);
    }
    if (null != setting) {
      configuration.setId(setting.getId());
      configuration.setModuleId(setting.getModuleId());
    }
    return setting;
  }

  private List<AutomationTestResult> readTestResults(AbstractBuild build, Launcher launcher, BuildListener listener, PrintStream logger, JunitSubmitter junitSubmitter) {
    List<AutomationTestResult> automationTestResults;
    long start = System.currentTimeMillis();
    LoggerUtils.formatHR(logger);
    try {
      automationTestResults = JunitTestResultParser.parse(new ParseRequest()
              .setBuild(build)
              .setWorkSpace(build.getWorkspace())
              .setLauncher(launcher)
              .setListener(listener)
              .setCreateEachMethodAsTestCase(configuration.getEachMethodAsTestCase())
              .setOverwriteExistingTestSteps(configuration.isOverwriteExistingTestSteps())
              .setUtilizeTestResultFromCITool(configuration.getReadFromJenkins())
              .setParseTestResultPattern(configuration.getResultPattern())
      );
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
      LoggerUtils.formatError(logger, e.getMessage());
      automationTestResults = Collections.emptyList();
    }
    if (automationTestResults.isEmpty()) {
      LoggerUtils.formatWarn(logger, "No JUnit test result found.");
      storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_SKIPPED);
      LoggerUtils.formatHR(logger);
      return Collections.emptyList();
    }
    LoggerUtils.formatInfo(logger, "JUnit test result found: %s, time elapsed: %s", automationTestResults.size(), LoggerUtils.elapsedTime(start));
    LoggerUtils.formatHR(logger);
    LoggerUtils.formatInfo(logger, "");
    return automationTestResults;
  }

  private JunitSubmitterResult submitTestResult(AbstractBuild build, BuildListener listener,
                                                JunitSubmitter junitSubmitter, List<AutomationTestResult> automationTestResults) {
    PrintStream logger = listener.getLogger();
    JunitSubmitterResult result = null;
    LoggerUtils.formatInfo(logger, "Begin submit test results to qTest at: " + JsonUtils.getCurrentDateString());
    long start = System.currentTimeMillis();
    try {
      result = junitSubmitter.submit(
        new JunitSubmitterRequest()
          .setConfiguration(configuration)
          .setTestResults(automationTestResults)
          .setBuildNumber(build.getNumber() + "")
          .setBuildPath(build.getUrl())
          .setListener(listener));
    } catch (SubmittedException e) {
      LoggerUtils.formatError(logger, "Cannot submit test results to qTest:");
      LoggerUtils.formatError(logger, "   status code: " + e.getStatus());
      LoggerUtils.formatError(logger, "   error: " + e.getMessage());
    } catch (Exception e) {
      LoggerUtils.formatError(logger, "Cannot submit test results to qTest:");
      LoggerUtils.formatError(logger, "   error: " + e.getMessage());
    } finally {
      if (null == result) {
        result = new JunitSubmitterResult()
          .setTestSuiteId(null)
          .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
          .setNumberOfTestResult(automationTestResults.size())
          .setNumberOfTestLog(0);
      }

      Boolean isSuccess = null != result.getTestSuiteId() && result.getTestSuiteId() > 0;
      LoggerUtils.formatHR(logger);
      LoggerUtils.formatInfo(logger, isSuccess ? "SUBMIT SUCCESS" : "SUBMIT FAILED");
      LoggerUtils.formatHR(logger);
      if (isSuccess) {
        int numberTestLog = 0 != result.getNumberOfTestLog() ? result.getNumberOfTestLog() : automationTestResults.size();
        LoggerUtils.formatInfo(logger, "   testLogs: %s", numberTestLog);
        LoggerUtils.formatInfo(logger, "   testSuite: name=%s, id=%s", result.getTestSuiteName(), result.getTestSuiteId());
        LoggerUtils.formatInfo(logger, "   link: %s", ConfigService.formatTestSuiteLink(configuration.getUrl(), configuration.getProjectId(), result.getTestSuiteId()));
      }
      LoggerUtils.formatInfo(logger, "Time elapsed: %s", LoggerUtils.elapsedTime(start));
      LoggerUtils.formatInfo(logger, "End submit test results to qTest at: %s", JsonUtils.getCurrentDateString());
      LoggerUtils.formatInfo(logger, "");
    }

    return result;
  }

  private void saveConfiguration(AbstractBuild build, JunitSubmitterResult result, PrintStream logger) {
    //set testSuite id created from qTest
    configuration.setTestSuiteId(null == result.getTestSuiteId() ? configuration.getTestSuiteId() : result.getTestSuiteId());
    try {
      build.getProject().save();
      LoggerUtils.formatInfo(logger, "Save test suite to configuration success.");
    } catch (IOException e) {
      LoggerUtils.formatError(logger, "Cannot save test suite to configuration of project:");
      LoggerUtils.formatError(logger, "   error:%s", e.getMessage());
      e.printStackTrace(logger);
    }
  }

  private void storeResult(AbstractBuild build, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
    try {
      junitSubmitter.storeSubmittedResult(build, result);
      LoggerUtils.formatInfo(logger, "Store submission result to workspace success.");
    } catch (Exception e) {
      LoggerUtils.formatError(logger, "Cannot store submission result: " + e.getMessage());
      e.printStackTrace(logger);
    }
    LoggerUtils.formatInfo(logger, "");
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
      configuration.setJenkinsServerUrl(HttpClientUtils.getServerUrl(req));
      configuration.setJenkinsProjectName(req.getParameter("name"));
      configuration.setSubmitToContainer(formData.getBoolean("submitToContainer"));
      configuration.setReadFromJenkins(formData.getBoolean("readFromJenkins"));
      configuration.setEachMethodAsTestCase(formData.getBoolean("eachMethodAsTestCase"));
      configuration.setContainerSetting(formData.getString("containerSetting"));
      configuration.setOverwriteExistingTestSteps(formData.getBoolean("overwriteExistingTestSteps"));
      configuration = ConfigService.validateConfiguration(configuration, formData);

      //if have url, we try to update configuration to qTest
      if (!StringUtils.isEmpty(configuration.getUrl())) {
        Setting setting = null;
        try {
          setting = ConfigService.saveConfiguration(configuration);
        } catch (Exception e) {
          LOG.log(Level.WARNING, e.getMessage());
          e.printStackTrace();
        }
        if (null != setting) {
          configuration.setModuleId(setting.getModuleId());
          configuration.setId(setting.getId());
        }
      }
      return new PushingResultAction(configuration);
    }

    public FormValidation doCheckUrl(@QueryParameter String value, @AncestorInPath AbstractProject project)
      throws IOException, ServletException {
      if (StringUtils.isEmpty(value))
        return FormValidation.error(ResourceBundle.MSG_INVALID_URL);
      try {
        new URL(value);
        Boolean isQtestUrl = ConfigService.validateQtestUrl(value);
        return isQtestUrl ? FormValidation.ok() : FormValidation.error(ResourceBundle.MSG_INVALID_URL);
      } catch (Exception e) {
        return FormValidation.error(ResourceBundle.MSG_INVALID_URL);
      }
    }

    public FormValidation doCheckAppSecretKey(@QueryParameter String value, @QueryParameter("config.url") final String url, @AncestorInPath AbstractProject project)
      throws IOException, ServletException {
      if (StringUtils.isEmpty(value) || StringUtils.isEmpty(url))
        return FormValidation.error(ResourceBundle.MSG_INVALID_API_KEY);
      if (!ConfigService.validateApiKey(url, value))
        return FormValidation.error(ResourceBundle.MSG_INVALID_API_KEY);
      return FormValidation.ok();
    }

    public FormValidation doCheckProjectName(@QueryParameter String value)
      throws IOException, ServletException {
      if (StringUtils.isBlank(value))
        return FormValidation.error(ResourceBundle.MSG_INVALID_PROJECT);
      return FormValidation.ok();
    }

    public FormValidation doCheckReleaseName(@QueryParameter String value)
      throws IOException, ServletException {
      if (StringUtils.isBlank(value))
        return FormValidation.error(ResourceBundle.MSG_INVALID_RELEASE);
      return FormValidation.ok();
    }

    public FormValidation doCheckEnvironment(@QueryParameter String value)
      throws IOException, ServletException {
      return FormValidation.ok();
    }

    public FormValidation doCheckResultPattern(@QueryParameter String value)
      throws IOException, ServletException {
      return FormValidation.ok();
    }
    public FormValidation doCheckFakeContainerName(@QueryParameter String value)
      throws IOException, ServletException {
//      if (!StringUtils.isBlank(value)) {
//        try {
//          JSONObject json = JSONObject.fromObject(value);
//          JSONObject selectedContainer = json.getJSONObject("selectedContainer");
//          if (selectedContainer.has("name")) {
//            if (!StringUtils.isBlank(selectedContainer.getString("name"))){
//              return FormValidation.ok();
//            }
//          }
//        } catch (Exception ex) {
//          ex.printStackTrace();
//        }
//      }
      if (!StringUtils.isBlank(value)) {
        return FormValidation.ok();
      }
      return FormValidation.error(ResourceBundle.MSG_INVALID_CONTAINER);
    }

    /**
     * @param qTestUrl qtest url
     * @param apiKey   api key
     * @return a list of project
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
     * @param qTestUrl           qTestUrl
     * @param apiKey             apiKey
     * @param projectId          projectId
     * @param jenkinsProjectName jenkinsProjectName
     * @return data
     */
    @JavaScriptMethod
    public JSONObject getProjectData(final String qTestUrl, final String apiKey, final Long projectId, final String jenkinsProjectName) {
      final JSONObject res = new JSONObject();
      final StaplerRequest request = Stapler.getCurrentRequest();
      final String jenkinsServerName = HttpClientUtils.getServerUrl(request);
      String token = null;
      try {
        token = OauthProvider.getAccessToken(qTestUrl, apiKey);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error while get projectData:" + e.getMessage());
      }
      final String accessToken = token;

      Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
      if (null == project) {
        //if project not found, we return empty data
        res.put("setting", "");
        res.put("releases", "");
        res.put("environments", "");
        res.put("testCycles", "");
        res.put("testSuites", "");
        return res;
      }
      final int threadCount = 5;
      final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
      ExecutorService fixedPool = Executors.newFixedThreadPool(threadCount);
      Callable<Object> caGetSetting = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            //get saved setting from qtest
            Object setting = ConfigService.getConfiguration(new Setting().setJenkinsServer(jenkinsServerName)
                .setJenkinsProjectName(jenkinsProjectName)
                .setProjectId(projectId)
                .setServerId(ConfigService.getServerId(jenkinsServerName)),
              qTestUrl, accessToken);
            res.put("setting", null == setting ? "" : JSONObject.fromObject(setting));
            return setting;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      Callable<Object> caGetReleases = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            Object releases = ConfigService.getReleases(qTestUrl, accessToken, projectId);
            res.put("releases", null == releases ? "" : JSONArray.fromObject(releases));
            return releases;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      Callable<Object> caGetTestCycles = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            Object testCycles = ConfigService.getTestCycleChildren(qTestUrl, accessToken, projectId, (long) 0, "root");
            res.put("testCycles", null == testCycles ? "" : JSONArray.fromObject(testCycles));
            return testCycles;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      Callable<Object> caGetTestSuites = new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        try {
            Object testSuites = ConfigService.getTestSuiteChildren(qTestUrl, accessToken, projectId, (long) 0, "root");
            res.put("testSuites", null == testSuites ? "" : JSONArray.fromObject(testSuites));
            return testSuites;
        } finally {
            countDownLatch.countDown();
        }
      }
      };
      Callable<Object> caGetEnvs = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
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
      fixedPool.submit(caGetTestCycles);
      fixedPool.submit(caGetTestSuites);

      try {
        countDownLatch.await();
      } catch (InterruptedException e) {
        LOG.log(Level.WARNING, e.getMessage());
      } finally {
        fixedPool.shutdownNow();
        return res;
      }
    }

    @JavaScriptMethod
    public JSONObject getContainerChildren(final  String qTestUrl, final String apiKey, final Long projectId, final Long parentId, final String parentType) {
      final JSONObject res = new JSONObject();
      String token = null;
      try {
        token = OauthProvider.getAccessToken(qTestUrl, apiKey);
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Error while get projectData:" + e.getMessage());
      }
      final String accessToken = token;

      Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
      if (null == project) {
        //if project not found, we return empty data
        res.put("testCycles", "");
        res.put("testSuites", "");
        return res;
      }
      final int threadCount = 2;
      final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
      ExecutorService fixedPool = Executors.newFixedThreadPool(threadCount);
      Callable<Object> caGetTestCycles = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            Object testCycles = ConfigService.getTestCycleChildren(qTestUrl, accessToken, projectId, parentId, parentType);
            res.put("testCycles", null == testCycles ? "" : JSONArray.fromObject(testCycles));
            return testCycles;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      Callable<Object> caGetTestSuites = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          try {
            Object testSuites = ConfigService.getTestSuiteChildren(qTestUrl, accessToken, projectId, parentId, parentType);
            res.put("testSuites", null == testSuites ? "" : JSONArray.fromObject(testSuites));
            return testSuites;
          } finally {
            countDownLatch.countDown();
          }
        }
      };
      fixedPool.submit(caGetTestCycles);
      fixedPool.submit(caGetTestSuites);

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
