package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.*;
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
    JunitSubmitterRequest submitterRequest = configuration.createJunitSubmitRequest();
    if (null == submitterRequest) {
      LoggerUtils.formatError(logger, "Could not create JUnitSumitterRequest");
      return true;
    }
    submitterRequest.setBuildNumber(build.getNumber() + "")
            .setBuildPath(build.getUrl())
            .setListener(listener);

    JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
    String buildResult = build.getResult() + "";
    if (Result.ABORTED.equals(build.getResult())) {
      LoggerUtils.formatWarn(logger, "Abort build action.");
      storeWhenNotSuccess(submitterRequest, junitSubmitter, build, buildResult, logger, JunitSubmitterResult.STATUS_CANCELED);
      return true;
    }
    showInfo(logger);
    if (!validateConfig(configuration)) {
      LoggerUtils.formatWarn(logger, "Invalid configuration to qTest, reject submit test results.");
      storeWhenNotSuccess(submitterRequest, junitSubmitter, build, buildResult,logger, JunitSubmitterResult.STATUS_FAILED);
      return true;
    }
    if (null == checkProjectNameChanged(build, listener)) {
      storeWhenNotSuccess(submitterRequest, junitSubmitter, build, buildResult, logger, JunitSubmitterResult.STATUS_CANCELED);
      return true;
    }
    List<AutomationTestResult> automationTestResults = readTestResults(build, launcher, listener, logger);
    if (automationTestResults.isEmpty()) {
      LoggerUtils.formatWarn(logger, "No JUnit test result found.");
      storeWhenNotSuccess(submitterRequest, junitSubmitter, build, buildResult, logger, JunitSubmitterResult.STATUS_SKIPPED);
      LoggerUtils.formatHR(logger);

      return true;
    }
    submitterRequest.setTestResults(automationTestResults);

    JunitSubmitterResult result = submitTestResult(submitterRequest, build, listener, junitSubmitter, automationTestResults);
    if (null == result) {
      //if have no test result, we do not break build flow
      return true;
    }
    saveConfiguration(build, result, logger);
    storeResult(submitterRequest, build, buildResult, junitSubmitter, result, logger);
    LoggerUtils.formatHR(logger);
    return true;
  }

  private Boolean storeWhenNotSuccess(JunitSubmitterRequest submitterRequest, JunitSubmitter junitSubmitter, AbstractBuild build, String buildResult, PrintStream logger, String status) {
    try {
      junitSubmitter.storeSubmittedResult(submitterRequest, build, buildResult, new JunitSubmitterResult()
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
      JSONObject json  = configuration.getContainerJSONObject();
      if (null != json) {
        JSONArray containerPath = json.optJSONArray("containerPath");
        if (null != containerPath) {
          int pathSize = containerPath.size();
          if (0 < pathSize) {
            JSONObject jsonNode = containerPath.optJSONObject(pathSize - 1);
            if (null != jsonNode) {
              Long nodeId = jsonNode.optLong("nodeId", 0L);
              return 0L != nodeId;
            }
          }
        }
      }
    }
    return false;
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
      Boolean saveOldSetting;
      saveOldSetting = ConfigService.compareqTestVersion(configuration.getUrl(), Constants.OLD_QTEST_VERSION);
      Setting settingFromConfig = configuration.toSetting(saveOldSetting);
      setting = ConfigService.saveConfiguration(configuration.getUrl(), configuration.getAppSecretKey(), settingFromConfig);
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

  private List<AutomationTestResult> readTestResults(AbstractBuild build, Launcher launcher, BuildListener listener, PrintStream logger) {
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
      return Collections.emptyList();
    }
    LoggerUtils.formatInfo(logger, "JUnit test result found: %s, time elapsed: %s", automationTestResults.size(), LoggerUtils.elapsedTime(start));
    LoggerUtils.formatHR(logger);
    LoggerUtils.formatInfo(logger, "");
    return automationTestResults;
  }

  private JunitSubmitterResult submitTestResult(JunitSubmitterRequest submitterRequest, AbstractBuild build, BuildListener listener,
                                                JunitSubmitter junitSubmitter, List<AutomationTestResult> automationTestResults) {
    PrintStream logger = listener.getLogger();
    JunitSubmitterResult result = null;
    LoggerUtils.formatInfo(logger, "Begin submit test results to qTest at: " + JsonUtils.getCurrentDateString());
    long start = System.currentTimeMillis();
    try {
      result = junitSubmitter.submit(submitterRequest);
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
        LoggerUtils.formatInfo(logger, "   testLogs: %s", result.getNumberOfTestLog());
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

  private void storeResult(JunitSubmitterRequest submitterRequest, AbstractBuild build, String buildResult, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
    try {
      junitSubmitter.storeSubmittedResult(submitterRequest, build, buildResult, result);
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
          Boolean saveOldSetting;
          saveOldSetting = ConfigService.compareqTestVersion(configuration.getUrl(), Constants.OLD_QTEST_VERSION);
          Setting settingFromConfig = configuration.toSetting(saveOldSetting);
          setting = ConfigService.saveConfiguration(configuration.getUrl(), configuration.getAppSecretKey(), settingFromConfig);
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
      return ValidationFormService.checkUrl(value, project);
    }

    public FormValidation doCheckAppSecretKey(@QueryParameter String value, @QueryParameter("config.url") final String url, @AncestorInPath AbstractProject project)
            throws IOException, ServletException {
      return ValidationFormService.checkAppSecretKey(value, url, project);
    }

    public FormValidation doCheckProjectName(@QueryParameter String value)
            throws IOException, ServletException {
      return ValidationFormService.checkProjectName(value);
    }

    public FormValidation doCheckReleaseName(@QueryParameter String value)
            throws IOException, ServletException {
      return ValidationFormService.checkReleaseName(value);
    }

    public FormValidation doCheckEnvironment(@QueryParameter String value)
            throws IOException, ServletException {
      return ValidationFormService.checkEnvironment(value);
    }

    public FormValidation doCheckResultPattern(@QueryParameter String value)
            throws IOException, ServletException {
      return ValidationFormService.checkResultPattern(value);
    }

    public FormValidation doCheckFakeContainerName(@QueryParameter String value) {
      return ValidationFormService.checkFakeContainerName(value);
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
      final StaplerRequest request = Stapler.getCurrentRequest();
      final String jenkinsServerName = HttpClientUtils.getServerUrl(request);
      return qTestService.getProjectData(qTestUrl, apiKey, projectId, jenkinsProjectName, jenkinsServerName);
    }

    @JavaScriptMethod
    public JSONObject getContainerChildren(final  String qTestUrl, final String apiKey, final Long projectId, final Long parentId, final String parentType) {
      return qTestService.getContainerChildren(qTestUrl, apiKey, projectId, parentId, parentType);
    }

    @JavaScriptMethod
    public JSONObject getQtestInfo(String qTestUrl) {
      return qTestService.getQtestInfo(qTestUrl);
    }
  }
}
