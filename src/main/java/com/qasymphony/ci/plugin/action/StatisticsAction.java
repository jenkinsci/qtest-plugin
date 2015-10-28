/**
 *
 */
package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.store.StoreResultServiceImpl;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Item;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anpham
 */
@ExportedBean
public class StatisticsAction extends Actionable implements Action {
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

  @JavaScriptMethod
  public JSONArray getNoOfBuilds(String noOfbuildsNeeded) {
    JSONArray jsonArray;
    int noOfBuilds = getNoOfBuildRequired(noOfbuildsNeeded);

    jsonArray = getBuildsArray(getBuildList(noOfBuilds));

    return jsonArray;
  }

  private JSONArray getBuildsArray(List<Integer> buildList) {
    JSONArray jsonArray = new JSONArray();
    for (Integer build : buildList) {
      jsonArray.add(build);
    }
    return jsonArray;
  }

  private List<Integer> getBuildList(int noOfBuilds) {
    if ((noOfBuilds <= 0) || (noOfBuilds >= builds.size())) {
      return builds;
    }
    List<Integer> buildList = new ArrayList<Integer>();
    for (int i = (noOfBuilds - 1); i >= 0; i--) {
      buildList.add(builds.get(i));
    }
    return buildList;
  }

  private int getNoOfBuildRequired(String noOfbuildsNeeded) {
    int noOfBuilds;
    try {
      noOfBuilds = Integer.parseInt(noOfbuildsNeeded);
    } catch (NumberFormatException e) {
      noOfBuilds = -1;
    }
    return noOfBuilds;
  }

  public boolean isUpdated() {
    int latestBuildNumber = project.getLastBuild().getNumber();
    return !(builds.contains(latestBuildNumber));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void getJsonLoadData() {

  }

  @Exported(name = "results", inline = true)
  public List<SubmittedResult> getResult() {
    return new ArrayList<>(getTreeResult(20).values());
  }

  @JavaScriptMethod
  public JSONObject getTreeResult(int page) {
    FilePath workspace = this.getProject().getWorkspace();
    try {
      results = storeResultService.fetchAll(workspace);
    } catch (Exception e) {
      e.printStackTrace();
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("data", results.values());
    return jsonObject;
  }
}
