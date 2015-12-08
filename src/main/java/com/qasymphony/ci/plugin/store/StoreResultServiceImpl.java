package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import com.qasymphony.ci.plugin.store.file.FileReader;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author trongle
 * @version 10/22/2015 9:20 PM trongle $
 * @since 1.0
 */
public class StoreResultServiceImpl implements StoreResultService {
  /**
   * Separate file by build number for each 20000 build
   */
  private static final Integer BREAK_FILE_BY = 20000;
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

  @Override public ReadSubmitLogResult fetch(ReadSubmitLogRequest request) throws StoreResultException {
    FilePath resultPath = getResultFolder(request.getProject());
    int numOrder = request.getCurrentBuildNumber() / BREAK_FILE_BY;
    List<FileReader> readerList = getReaderList(resultPath, numOrder);

    //get saved configuration
    Configuration configuration = ConfigService.getPluginConfiguration(request.getProject());
    String qTestUrl = configuration == null ? "" : configuration.getUrl();
    Long projectId = configuration == null ? 0L : configuration.getProjectId();
    int total = 0;
    Map<Integer, SubmittedResult> resultMap = new HashMap<>();

    for (FileReader reader : readerList) {
      total += reader.size();
      try {
        resultMap.putAll(buildSubmittedResult(reader.readAll(), qTestUrl, projectId));
      } catch (IOException e) {
        throw new StoreResultException(e.getMessage(), e);
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          throw new StoreResultException(e.getMessage(), e);
        }
      }
    }

    return new ReadSubmitLogResult()
      .setTotal(total)
      .setResults(resultMap);
  }

  private static List<FileReader> getReaderList(FilePath resultPath, int numOrder) {
    List<FileReader> readerList = new ArrayList<>();
    if (numOrder <= 0) {
      FilePath resultFile = getResultFile(resultPath, numOrder);
      try {
        readerList.add(new FileReader(new File(resultFile.getName())));
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      for (int i = 0; i < numOrder; i++) {
        FilePath resultFile = getResultFile(resultPath, numOrder);
        try {
          readerList.add(new FileReader(new File(resultFile.getName())));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return readerList;
  }

  @Override public ReadSubmitLogResult fetchAll(ReadSubmitLogRequest request)
    throws StoreResultException {
    FilePath resultPath = getResultFolder(request.getProject());
    Map<Integer, SubmittedResult> buildResults = new HashMap<>();
    int numOrder = request.getCurrentBuildNumber() / BREAK_FILE_BY;
    //get saved configuration
    Configuration configuration = ConfigService.getPluginConfiguration(request.getProject());
    String qTestUrl = configuration == null ? "" : configuration.getUrl();
    Long projectId = configuration == null ? 0L : configuration.getProjectId();
    try {
      if (numOrder <= 0) {
        FilePath resultFile = getResultFile(resultPath, numOrder);
        buildResults.putAll(readResult(resultFile, qTestUrl, projectId));
      } else {
        for (int i = 0; i < numOrder; i++) {
          FilePath resultFile = getResultFile(resultPath, i);
          buildResults.putAll(readResult(resultFile, qTestUrl, projectId));
        }
      }
    } catch (Exception e) {
      throw new StoreResultException(e);
    }
    return new ReadSubmitLogResult()
      .setResults(buildResults)
      .setTotal(buildResults.size());
  }

  private static Map<Integer, SubmittedResult> readResult(FilePath resultFile, String url, Long projectId)
    throws StoreResultException {
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

    return buildSubmittedResult(lines, url, projectId);
  }

  private static Map<Integer, SubmittedResult> buildSubmittedResult(Map<Integer, String> lines, String url, Long projectId) {
    Map<Integer, SubmittedResult> buildResults = new HashMap<>();
    for (Map.Entry<Integer, String> entry : lines.entrySet()) {
      SubmittedResult submitResult = JsonUtils.fromJson(entry.getValue(), SubmittedResult.class);
      if (null != submitResult) {
        if (StringUtils.isEmpty(submitResult.getUrl())) {
          submitResult.setTestSuiteLink(ConfigService.formatTestSuiteLink(url, projectId, submitResult.getTestSuiteId()));
        } else {
          submitResult.setTestSuiteLink(ConfigService.formatTestSuiteLink(submitResult.getUrl(), submitResult.getProjectId(), submitResult.getTestSuiteId()));
        }
        buildResults.put(submitResult.getBuildNumber(), submitResult);
      }
    }
    return buildResults;
  }

  private static FilePath getResultFolder(AbstractProject project) {
    FilePath projectFolder = new FilePath(project.getConfigFile().getFile()).getParent();
    return new FilePath(projectFolder, RESULT_FOLDER);
  }

  private static FilePath getResultFile(FilePath resultFolder, int fileOrder) {
    return new FilePath(resultFolder, String.format("%s_%s%s", RESULT_FILE, fileOrder, RESULT_FILE_EXT));
  }
}
