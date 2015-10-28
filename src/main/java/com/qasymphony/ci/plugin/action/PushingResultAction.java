/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.SubmitResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import hudson.Extension;
import hudson.FilePath;
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
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
    descriptor.setConfiguration(configuration);
    return descriptor;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
    throws InterruptedException, IOException {
    PrintStream logger = listener.getLogger();
    logger.println("Submit Junit to qTest at:" + configuration.getUrl() + ".");

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", configuration.getAppSecretKey());
    try {
      ResponseEntity response = HttpClientUtils.get(configuration.getUrl() + "/version", headers);
      LOG.info(response.getBody());
      logger.println(response.getBody());
    } catch (Exception e) {
      logger.println(e.getMessage());
    }

    //TODO: collect test result and submit to qTest here.

    JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();

    try {
      AutomationTestService.push(null, build, build.getWorkspace(), launcher, listener, configuration, headers);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    JunitSubmitterResult junitSubmitterResult = junitSubmitter.submit(
      new JunitSubmitterRequest().withSetting(configuration));

    logger.println("Configuration:" + configuration);
    logger.println("Project:" + build.getProject().getName() + ", previous testSuite:" + configuration.getTestSuiteId());

    configuration.setTestSuiteId(junitSubmitterResult.getTestSuiteId());
    try {
      build.getProject().save();
      LOG.info("Save project config with suite:" + configuration.getTestSuiteId());
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Cannot save project with suite:" + e.getMessage());
    }
    final FilePath filePath = build.getWorkspace();
    StoreResultService storeResultService = new StoreResultServiceImpl();
    SubmitResult submitResult = new SubmitResult()
      .setBuildNumber(build.getNumber())
      .setSubmitStatus("SUCCESS")
      .setStatusBuild(build.getResult().toString())
      .setTestSuiteName(build.getProject().getName())
      .setNumberTestRun(1)
      .setNumberTestResult(1);
    storeResultService.store(filePath, submitResult);
    logger.println(filePath.toURI());
    return true;
  }

  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private Configuration configuration;

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

    @SuppressWarnings("deprecation")
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
      Configuration configuration = req.bindParameters(Configuration.class, "config.");
      configuration.setJenkinsServerUrl(getServerUrl(req));
      configuration.setJenkinsProjectName(req.getParameter("name"));

      Long moduleId = ConfigService.saveConfiguration(configuration);
      if (null != moduleId)
        configuration.setModuleId(moduleId);
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

    public FormValidation doCheckAppSecretKey(@QueryParameter String value, @QueryParameter String url)
      throws IOException, ServletException {
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
    public static JSONObject getProjects(String qTestUrl, String apiKey) {
      JSONObject res = new JSONObject();
      //TODO: need apply executor
      //get project from qTest
      Object projects = ConfigService.getProjects(qTestUrl, apiKey);
      res.put("projects", null == projects ? "" : JSONArray.fromObject(projects));
      //get saved setting from qtest
      Object setting = ConfigService.getConfiguration(qTestUrl, apiKey);
      res.put("setting", null == setting ? "" : JSONArray.fromObject(setting));
      JSONObject obj = new JSONObject();
      return res;
    }

    /**
     * @param qTestUrl
     * @param apiKey
     * @param projectId
     * @return
     */
    @JavaScriptMethod
    public static JSONObject getProjectData(String qTestUrl, String apiKey, Long projectId) {
      JSONObject res = new JSONObject();
      Object releases = ConfigService.getReleases(qTestUrl, apiKey, projectId);
      res.put("releases", null == releases ? "" : JSONArray.fromObject(releases));
      Object environments = ConfigService.getEnvironments(qTestUrl, apiKey, projectId);
      res.put("environments", null == environments ? "" : environments);
      return res;
    }

    public Configuration getConfiguration() {
      return configuration;
    }

    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }

  }
}
