/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.ResourceBundle;
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
import java.util.Date;
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
    logger.println("");
    logger.println("---------------------------------------------------------------------------------------------");
    logger.println(String.format("[INFO] Submit Junit to qTest at:%s, project:%s.", configuration.getUrl(), configuration.getProjectId()));
    logger.println("[INFO] " + configuration);
    validateJobName(build, logger);

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

  private void validateJobName(AbstractBuild build, PrintStream logger) {
    String currentJenkinProjectName = build.getProject().getName();
    if (!configuration.getJenkinsProjectName().equals(currentJenkinProjectName)) {
      logger.println(String.format("[INFO] Current job name [%s] is changed with previous configuration.", currentJenkinProjectName));
      configuration.setJenkinsProjectName(currentJenkinProjectName);
      ConfigService.saveConfiguration(configuration);
    }
  }

  private JunitSubmitterResult submitTestResult(AbstractBuild build, Launcher launcher, BuildListener listener, PrintStream logger, JunitSubmitter junitSubmitter) {
    TestResultParse testResultParse = new MavenJunitParse(build, launcher, listener);
    List<AutomationTestResult> automationTestResults;
    try {
      automationTestResults = testResultParse.parse();
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
      automationTestResults = Collections.emptyList();
    }

    if (automationTestResults.isEmpty()) {
      logger.println("[ERROR] No junit test result found.");
      return null;
    }
    logger.println(String.format("[INFO] Junit test result found: %s", automationTestResults.size()));

    JunitSubmitterResult result = null;
    logger.println("[INFO] Begin submit test result to qTest, start at:" + new Date().toString());
    try {
      result = junitSubmitter.submit(
        new JunitSubmitterRequest()
          .setConfiguration(configuration)
          .setTestResults(automationTestResults)
          .setBuildId(build.getId())
          .setBuildPath(build.getUrl()));
    } catch (Exception e) {
      logger.println("[ERROR] Cannot submit test result to qTest:" + e.getMessage());
      result = new JunitSubmitterResult()
        .setTestSuiteId(null)
        .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
        .setNumberOfTestResult(automationTestResults.size())
        .setNumberOfTestRun(0);
    } finally {
      logger.println(String.format("[INFO] End submit test result to qTest at=%s, testRuns=%s, testResult=%s",
        new Date().toString(),
        result.getNumberOfTestRun(),
        result.getNumberOfTestResult()));
    }
    return result;
  }

  private void saveConfiguration(AbstractBuild build, JunitSubmitterResult result, PrintStream logger) {
    //set testSuite id created from qTest
    configuration.setTestSuiteId(null == result.getTestSuiteId() ? configuration.getTestSuiteId() : result.getTestSuiteId());
    try {
      build.getProject().save();
      logger.println(String.format("[INFO] Save test suite to configuration, testSuiteId=%s", configuration.getTestSuiteId()));
    } catch (IOException e) {
      logger.println(String.format("[ERROR] Cannot save test suite to configuration of project:%s", e.getMessage()));
    }
  }

  private void storeResult(AbstractBuild build, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
    logger.println("[INFO] Begin store submitted result to workspace.");
    try {
      junitSubmitter.storeSubmittedResult(build, result);
    } catch (Exception e) {
      logger.println("[ERROR] Cannot store submitted result." + e.getMessage());
    }
    logger.println("[INFO] End store submitted result.");
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
