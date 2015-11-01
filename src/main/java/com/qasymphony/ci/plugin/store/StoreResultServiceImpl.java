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
  private static final String RESULT_FILE = "submitted";

  /**
   * Extension
   */
  private static final String RESULT_FILE_EXT = ".result";

  /**
   * Separate file by build number for each 20000 build
   */
  public static final Integer BREAK_FILE_BY = 20000;

  @Override public Boolean store(AbstractProject project, final SubmittedResult result)
    throws StoreResultException {
    FilePath resultFolder = getResultFolder(project);
    try {
      resultFolder.mkdirs();
    } catch (Exception e) {
      throw new StoreResultException(String.format("Cannot make result folder:%s, %s", resultFolder.getName(), e.getMessage()));
    }
    FilePath resultFile = getResultFile(resultFolder, result.getBuildNumber() / BREAK_FILE_BY);

    try {
      resultFile.act(new FilePath.FileCallable<String>() {
        @Override public String invoke(File file, VirtualChannel virtualChannel)
          throws IOException, InterruptedException {
          BufferedWriter writer = null;
          try {
            writer = new BufferedWriter(new FileWriter(file.getPath(), true));
            writer.write(JsonUtils.toJson(result));
            writer.newLine();
            return null;
          } finally {
            if (null != writer)
              writer.close();
          }
        }

        @Override public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        }
      });
    } catch (Exception e) {
      throw new StoreResultException("Cannot store result to file:" + e.getMessage());
    }
    return true;
  }

  @Override public Map<Integer, SubmittedResult> fetchAll(AbstractProject project, int currentBuildNumber)
    throws StoreResultException {
    FilePath resultPath = getResultFolder(project);
    Map<Integer, SubmittedResult> buildResults = new HashMap<>();
    try {
      int numOrder = currentBuildNumber / BREAK_FILE_BY;
      if (numOrder <= 0) {
        FilePath resultFile = getResultFile(resultPath, numOrder);
        buildResults.putAll(readResult(resultFile));
      } else {
        for (int i = 0; i < numOrder; i++) {
          FilePath resultFile = getResultFile(resultPath, i);
          buildResults.putAll(readResult(resultFile));
        }
      }
    } catch (Exception e) {
      throw new StoreResultException(e);
    }
    return buildResults;
  }

  private Map<Integer, SubmittedResult> readResult(FilePath resultFile) throws StoreResultException {
    Map<Integer, SubmittedResult> buildResults = new HashMap<>();
    SortedMap<Integer, String> lines = null;
    try {
      lines = resultFile.act(new FilePath.FileCallable<SortedMap<Integer, String>>() {
        @Override public SortedMap<Integer, String> invoke(File file, VirtualChannel virtualChannel)
          throws IOException, InterruptedException {
          FileReader fileReader = new FileReader(file);
          SortedMap<Integer, String> lines;
          try {
            lines = fileReader.readAll();
          } finally {
            if (null != fileReader)
              fileReader.close();
          }
          return lines;
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

  private FilePath getResultFile(FilePath resultFolder, int fileOrder) {
    return new FilePath(resultFolder, String.format("%s_%s%s", RESULT_FILE, fileOrder, RESULT_FILE_EXT));
  }
}
