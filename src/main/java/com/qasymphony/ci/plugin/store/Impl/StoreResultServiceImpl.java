package com.qasymphony.ci.plugin.store.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasymphony.ci.plugin.model.SubmitResult;
import com.qasymphony.ci.plugin.store.StoreResultService;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

  @Override public Boolean store(FilePath workspace, final Object result) throws IOException, InterruptedException {
    FilePath projectPath = workspace.getParent();
    FilePath resultFolder = new FilePath(projectPath, RESULT_FOLDER);
    resultFolder.mkdirs();
    FilePath resultFile = new FilePath(resultFolder, RESULT_FILE);

    resultFile.act(new FilePath.FileCallable<String>() {
      @Override public String invoke(File file, VirtualChannel virtualChannel)
        throws IOException, InterruptedException {
        file.createNewFile();
        FileWriter wr = new FileWriter(file.getPath());
        wr.write(JsonUtils.toJson(result));
        wr.close();
        return null;
      }

      @Override public void checkRoles(RoleChecker roleChecker) throws SecurityException {

      }
    });
    return true;
  }

  @Override public String load(FilePath workspace) throws IOException, InterruptedException {
    FilePath projectPath = workspace.getParent();
    FilePath resultFolder = new FilePath(projectPath, RESULT_FOLDER);
    FilePath resultFile = new FilePath(resultFolder, RESULT_FILE);
    final String data = "";
    JsonNode node = JsonUtils.read(resultFile.read());
    return node == null ? "" : node.toString();
  }

  @Override public Map<Long, SubmitResult> fetchAll(FilePath filePath) {
    Map<Long, SubmitResult> buildResults = new HashMap<>();
    buildResults.put(1L, new SubmitResult()
      .setBuildNumber(1L)
      .setTestSuiteName("Test Suite ")
      .setStatusBuild("SUCCESS")
      .setSubmitStatus("SUCCESS"));
    return buildResults;
  }
}
