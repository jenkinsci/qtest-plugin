package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import hudson.FilePath;

import java.io.IOException;
import java.util.Map;

/**
 * @author trongle
 * @version 10/22/2015 9:20 PM trongle $
 * @since 1.0
 */
public interface StoreResultService {
  Boolean store(FilePath workspace, Object result) throws StoreResultException;

  String load(FilePath workspace) throws StoreResultException;

  Map<Integer, SubmittedResult> fetchAll(FilePath filePath) throws StoreResultException;
}
