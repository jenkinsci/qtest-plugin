/**
 * 
 */
package com.qasymphony.ci.plugin.action;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.Configuration;

/**
 * @author anpham
 *
 */
public class PushingResultAction extends Notifier {
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
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
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
      return ResourceBundle.INSTANCE.get(ResourceBundle.DISPLAY_NAME);
    }
    
    @Override
    public String getHelpFile() {
      return "/plugin/jqtest/help/main.html";
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
      Configuration configuration = req.bindParameters(Configuration.class, "com.qasymphony.ci.plugin.configuration.");
      return new PushingResultAction(configuration);
    }
    
    @JavaScriptMethod
    public static JSONObject getProjects(String qTestUrl, String appKey) {
      //TODO: get project from qTest
      JSONObject res = new JSONObject();
      return res;
    }
  }
}
