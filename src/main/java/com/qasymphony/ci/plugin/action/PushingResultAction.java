/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.submitter.Impl.JunitQtestSubmitterImpl;
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
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    return (DescriptorImpl) super.getDescriptor();
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
      HttpResponse response = HttpClientUtils.get(configuration.getUrl() + "/version", headers);
      if (response != null) {
        BufferedReader rd = new BufferedReader(
          new InputStreamReader(response.getEntity().getContent()));

        StringBuilder result = new StringBuilder();
        String line = "";
        while ((line = rd.readLine()) != null) {
          result.append(line);
        }
        LOG.info(result.toString());
      }
    } catch (Exception e) {
      logger.print(e.getMessage());
    }

    //TODO: collect test result and submit to qTest here.

    JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
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

    @SuppressWarnings("deprecation")
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
      Configuration configuration = req.bindParameters(Configuration.class, "config.");
      return new PushingResultAction(configuration);
    }

    public FormValidation doCheckUrl(@QueryParameter String value)
      throws IOException, ServletException {
      try {
        new URL(value);
        return FormValidation.ok();
      } catch (Exception e) {
        return FormValidation.error("Please set a valid qTest URL");
      }
    }

    public FormValidation doCheckAppSecretKey(@QueryParameter String value)
      throws IOException, ServletException {
      if (value.length() <= 0)
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
      if (value.length() <= 0)
        return FormValidation.error("Please select a environment.");
      return FormValidation.ok();
    }

    @JavaScriptMethod
    public static JSONObject getProjects(String qTestUrl, String appKey) {
      //TODO: get project from qTest
      JSONObject res = new JSONObject();
      return res;
    }
  }
}
