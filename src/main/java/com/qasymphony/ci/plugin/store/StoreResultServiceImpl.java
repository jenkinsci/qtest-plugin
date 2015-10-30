package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.file.FileReader;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author trongle
 * @version 10/22/2015 9:20 PM trongle $
 * @since 1.0
 */
public class StoreResultServiceImpl implements StoreResultService {
  /**
   * <p>
   * Folder store data for plugin, we make folder located in jobs/projectName
   * </p>
   */
  private static final String RESULT_FOLDER = "jqtest_results";
  /**
   * Result file
   */
  private static final String RESULT_FILE = "submitted.result";

  @Override public Boolean store(AbstractProject project, final Object result)
    throws StoreResultException {
    FilePath resultFolder = getResultFolder(project);
    try {
      resultFolder.mkdirs();
    } catch (Exception e) {
      throw new StoreResultException(String.format("Cannot make result folder:%s, %s", resultFolder.getName(), e.getMessage()));
    }
    FilePath resultFile = getResultFile(resultFolder);

    try {
      resultFile.act(new FilePath.FileCallable<String>() {
        @Override public String invoke(File file, VirtualChannel virtualChannel)
          throws IOException, InterruptedException {
          BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath(), true));
          writer.newLine();
          writer.write(JsonUtils.toJson(result));
          writer.close();
          return null;
        }

        @Override public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        }
      });
    } catch (Exception e) {
      throw new StoreResultException("Cannot store result to file:" + e.getMessage());
    }
    return true;
  }

  @Override public Map<Integer, SubmittedResult> fetchAll(AbstractProject project)
    throws StoreResultException {
    Map<Integer, SubmittedResult> buildResults = new HashMap<>();
    FilePath resultPath = getResultFolder(project);
    FilePath resultFile = getResultFile(resultPath);
    SortedMap<Integer, String> lines = null;
    try {
      lines = resultFile.act(new FilePath.FileCallable<SortedMap<Integer, String>>() {
        @Override public SortedMap<Integer, String> invoke(File file, VirtualChannel virtualChannel)
          throws IOException, InterruptedException {
          return new FileReader(file).readAll();
        }

        @Override public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }
      });
    } catch (Exception e) {
      throw new StoreResultException(String.format("Cannot read from result file:%s, %s", resultFile.getName(), e.getMessage()));
    }
    for (Map.Entry<Integer, String> entry : lines.entrySet()) {
      SubmittedResult submitResult = JsonUtils.fromJson(entry.getValue(), SubmittedResult.class);
      if (null != submitResult)
        buildResults.put(submitResult.getBuildNumber(), submitResult);
    }
    return buildResults;
  }

  private FilePath getResultFolder(AbstractProject project) {
    FilePath projectFolder = new FilePath(project.getConfigFile().getFile()).getParent();
    return new FilePath(projectFolder, RESULT_FOLDER);
  }

  private FilePath getResultFile(FilePath resultFolder) {
    return new FilePath(resultFolder, RESULT_FILE);
  }
}
