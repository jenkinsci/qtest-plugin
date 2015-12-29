package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.ReadSubmitLogRequest;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Item;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author anpham
 */
@ExportedBean
public class StatisticsAction extends Actionable implements Action {
  private static final Logger LOG = Logger.getLogger(StatisticsAction.class.getName());

  private AbstractProject project;
  private final StoreResultService storeResultService = new StoreResultServiceImpl();

  public StatisticsAction(AbstractProject project) {
    this.project = project;
  }

  /**
   * The display name for the action.
   *
   * @return the name as String
   */
  public final String getDisplayName() {
    return this.hasPermission() ? ResourceBundle.EXT_DISPLAY_NAME : null;
  }

  /**
   * The icon for this action.
   *
   * @return the icon file as String
   */
  public final String getIconFileName() {
    return this.hasPermission() ? ResourceBundle.EXT_DISPLAY_ICON : null;
  }

  /**
   * The url for this action.
   *
   * @return the url as String
   */
  public String getUrlName() {
    return this.hasPermission() ? ResourceBundle.EXT_URL_NAME : null;
  }

  /**
   * Search url for this action.
   *
   * @return the url as String
   */
  public String getSearchUrl() {
    return this.hasPermission() ? ResourceBundle.EXT_URL_SEARCH_NAME : null;
  }

  /**
   * Checks if the user has READ permission
   *
   * @return true - user has permission, false - no permission.
   */
  private boolean hasPermission() {
    return project.hasPermission(Item.READ);
  }

  @SuppressWarnings("rawtypes")
  public AbstractProject getProject() {
    return this.project;
  }

  /**
   * use to get result in qTest Plugin page
   *
   * @param page
   * @return
   */
  @JavaScriptMethod
  public JSONObject getTreeResult(int page) {
    Map<Integer, SubmittedResult> results = null;
    try {
      AbstractProject project = this.getProject();
      results = storeResultService.fetchAll(new ReadSubmitLogRequest()
        .setProject(project)
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
