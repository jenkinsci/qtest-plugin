package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.ReadSubmitLogRequest;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.model.Action;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.model.Job;

/**
 * @author tamvo
 */
@ExportedBean
public class PipelineStatisticsAction implements Action {
  private static final Logger LOG = Logger.getLogger(PipelineStatisticsAction.class.getName());

  WorkflowJob job;

  private final StoreResultService storeResultService = new StoreResultServiceImpl();

  public PipelineStatisticsAction(WorkflowJob job) {
    this.job = job;
  }

  /**
   * The display name for the action.
   *
   * @return the name as String
   */
  public final String getDisplayName() {
    return ResourceBundle.EXT_DISPLAY_NAME;
  }

  /**
   * The icon for this action.
   *
   * @return the icon file as String
   */
  public final String getIconFileName() {
    return ResourceBundle.EXT_DISPLAY_ICON;
  }

  /**
   * The url for this action.
   *
   * @return the url as String
   */
  public String getUrlName() {
    return ResourceBundle.EXT_URL_NAME;
  }

  /**
   * Search url for this action.
   *
   * @return the url as String
   */
  public String getSearchUrl() {
    return ResourceBundle.EXT_URL_SEARCH_NAME;
  }

  public WorkflowJob getJob() {
    return job;
  }

  /**
   * use to get result in qTest Plugin page
   *
   * @param page page submitted result
   * @return submitted result data
   */
  @JavaScriptMethod
  public JSONObject getTreeResult(int page) {
    Map<Integer, SubmittedResult> results = null;
    try {
      Job job = this.getJob();
      results = storeResultService.fetchAll(new ReadSubmitLogRequest()
        .setJob(job)
        .setStart(0)
        .setSize(-1))
        .getResults();
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("data", null == results ? "" : results.values());
    return jsonObject;
  }
}
