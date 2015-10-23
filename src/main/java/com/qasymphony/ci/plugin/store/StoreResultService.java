package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.model.SubmitResult;
import hudson.FilePath;

import java.io.IOException;
import java.util.Map;

/**
 * @author trongle
 * @version 10/22/2015 9:20 PM trongle $
 * @since 1.0
 */
public interface StoreResultService {
  Boolean store(FilePath workspace, Object result) throws IOException, InterruptedException;

  String load(FilePath workspace) throws IOException, InterruptedException;

  Map<Integer, SubmitResult> fetchAll(FilePath filePath) throws IOException, InterruptedException;
}
