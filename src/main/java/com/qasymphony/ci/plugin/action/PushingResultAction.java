/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.google.common.base.Stopwatch;
import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.parse.MavenJunitParse;
import com.qasymphony.ci.plugin.parse.TestResultParse;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
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
import java.util.concurrent.TimeUnit;
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
    logger.println("");
    logger.println("---------------------------------------------------------------------------------------------");
    logger.println(String.format("[INFO] Submit Junit to qTest at:%s, project:%s.", configuration.getUrl(), configuration.getProjectId()));
    logger.println("[INFO] " + configuration);
    logger.println("[INFO] Project:" + build.getProject().getName() + ", previous testSuite:" + configuration.getTestSuiteId());

    TestResultParse testResultParse = new MavenJunitParse(build, launcher, listener, configuration);
    List<AutomationTestResult> automationTestResults;
    try {
      automationTestResults = testResultParse.parse();
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
      automationTestResults = Collections.emptyList();
    }

    if (automationTestResults.isEmpty()) {
      logger.println("[ERROR] No junit test result found.");
      return true;
    }
    logger.println(String.format("[INFO] Junit test result found: %s", automationTestResults.size()));

    JunitSubmitterResult result = null;
    Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();
    logger.println("[INFO] Begin submit test result to qTest.");
    JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
    try {
      result = junitSubmitter.submit(
        new JunitSubmitterRequest()
          .setConfiguration(configuration)
          .setTestResults(automationTestResults));
    } catch (Exception e) {
      logger.println("[ERROR] Cannot submit test result to qTest:" + e.getMessage());
      result = new JunitSubmitterResult()
        .setTestSuiteId(null)
        .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
        .setNumberOfTestResult(automationTestResults.size())
        .setNumberOfTestRun(0);
    } finally {
      stopwatch.stop();
      logger.println(String.format("[INFO] End submit test result to qTest: time=%s (s), testRuns=%s, testResult=%s",
        stopwatch.elapsedTime(TimeUnit.SECONDS),
        result.getNumberOfTestRun(),
        result.getNumberOfTestResult()));
    }

    //set testSuite id created from qTest
    configuration.setTestSuiteId(null == result.getTestSuiteId() ? configuration.getTestSuiteId() : result.getTestSuiteId());
    try {
      build.getProject().save();
      logger.println(String.format("[INFO] Save test suite to configuration, testSuiteId=%s", configuration.getTestSuiteId()));
    } catch (IOException e) {
      logger.println(String.format("[ERROR] Cannot save test suite to configuration of project:%s", e.getMessage()));
    }

    logger.println("[INFO] Begin store submitted result to workspace.");
    try {
      junitSubmitter.storeSubmittedResult(build, result);
    } catch (StoreResultException e) {
      logger.println("[ERROR] Cannot store submitted result." + e.getMessage());
    }
    logger.println("[INFO] End store submitted result.");
    return true;
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      super(PushingResultAction.class);
      load();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return ResourceBundle.get(ResourceBundle.DISPLAY_NAME);
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
      //TODO: need apply executor
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
    public JSONObject getProjectData(String qTestUrl, String apiKey, Long projectId, String jenkinsProjectName) {
      JSONObject res = new JSONObject();
      StaplerRequest request = Stapler.getCurrentRequest();
      String jenkinsServerName = getServerUrl(request);

      //get saved setting from qtest
      Object setting = ConfigService.getConfiguration(qTestUrl, apiKey, jenkinsServerName,
        HttpClientUtils.encode(jenkinsProjectName), projectId);
      res.put("setting", null == setting ? "" : JSONObject.fromObject(setting));

      Object releases = ConfigService.getReleases(qTestUrl, apiKey, projectId);
      res.put("releases", null == releases ? "" : JSONArray.fromObject(releases));

      Object environments = ConfigService.getEnvironments(qTestUrl, apiKey, projectId);
      res.put("environments", null == environments ? "" : environments);
      return res;
    }
  }
}
