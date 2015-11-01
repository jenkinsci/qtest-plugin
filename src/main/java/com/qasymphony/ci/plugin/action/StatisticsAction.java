/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Item;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author anpham
 */
@ExportedBean
public class StatisticsAction extends Actionable implements Action {
  private static final Logger LOG = Logger.getLogger(StatisticsAction.class.getName());

  @SuppressWarnings("rawtypes") AbstractProject project;
  private List<Integer> builds = new ArrayList<Integer>();
  private StoreResultService storeResultService = new StoreResultServiceImpl();
  private Map<Integer, SubmittedResult> results = new HashMap<>();

  public StatisticsAction(@SuppressWarnings("rawtypes") AbstractProject project) {
    this.project = project;
  }

  /**
   * The display name for the action.
   *
   * @return the name as String
   */
  public final String getDisplayName() {
    return this.hasPermission() ? ResourceBundle.get("extension.name") : null;
  }

  /**
   * The icon for this action.
   *
   * @return the icon file as String
   */
  public final String getIconFileName() {
    return this.hasPermission() ? ResourceBundle.get("extension.icon") : null;
  }

  /**
   * The url for this action.
   *
   * @return the url as String
   */
  public String getUrlName() {
    return this.hasPermission() ? ResourceBundle.get("extension.url.name") : null;
  }

  /**
   * Search url for this action.
   *
   * @return the url as String
   */
  public String getSearchUrl() {
    return this.hasPermission() ? ResourceBundle.get("extension.url.search") : null;
  }

  /**
   * Checks if the user has CONFIGURE permission.
   *
   * @return true - user has permission, false - no permission.
   */
  private boolean hasPermission() {
    return project.hasPermission(Item.BUILD);
  }

  @SuppressWarnings("rawtypes")
  public AbstractProject getProject() {
    return this.project;
  }

  @Exported(name = "results", inline = true)
  public List<SubmittedResult> getResult() {
    return new ArrayList<>(getTreeResult(20).values());
  }

  @JavaScriptMethod
  public JSONObject getTreeResult(int page) {
    try {
      AbstractProject project = this.getProject();
      results = storeResultService.fetchAll(project, Math.max(project.getNextBuildNumber() - 1, 0));
    } catch (Exception e) {
      LOG.log(Level.WARNING, e.getMessage());
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("data", results.values());
    return jsonObject;
  }
}
