package com.qasymphony.ci.plugin.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.file.FileReader;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
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
   * Folder store data for plugin
   */
  private static final String RESULT_FOLDER = "jqtest_result";

  /**
   * File name contain data submitted to qTest
   */
  private static final String RESULT_FILE = "result";

  @Override public Boolean store(FilePath workspace, final Object result) throws StoreResultException {
    FilePath projectPath = workspace.getParent();
    FilePath resultFolder = new FilePath(projectPath, RESULT_FOLDER);
    try {
      resultFolder.mkdirs();
    } catch (Exception e) {
      throw new StoreResultException(String.format("Cannot make dir:%s, %s", resultFolder.getName(), e.getMessage()));
    }
    FilePath resultFile = new FilePath(resultFolder, RESULT_FILE);

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

  @Override public String load(FilePath workspace) throws StoreResultException {
    FilePath projectPath = workspace.getParent();
    FilePath resultFolder = new FilePath(projectPath, RESULT_FOLDER);
    FilePath resultFile = new FilePath(resultFolder, RESULT_FILE);
    InputStream in = null;
    try {
      in = resultFile.read();
    } catch (Exception e) {
      throw new StoreResultException(String.format("Cannot read from file: %s, %s", resultFile.getName(), e.getMessage()));
    }
    JsonNode node = JsonUtils.read(in);
    return node == null ? "" : node.toString();
  }

  @Override public Map<Integer, SubmittedResult> fetchAll(FilePath filePath) throws StoreResultException {
    Map<Integer, SubmittedResult> buildResults = new HashMap<>();
    FilePath resultPath = new FilePath(filePath.getParent(), RESULT_FOLDER);
    FilePath resultFile = new FilePath(resultPath, RESULT_FILE);
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
}
