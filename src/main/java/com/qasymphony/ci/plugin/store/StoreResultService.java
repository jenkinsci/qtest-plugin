package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import hudson.model.AbstractProject;

import java.util.Map;

/**
 * @author trongle
 * @version 10/22/2015 9:20 PM trongle $
 * @since 1.0
 */
public interface StoreResultService {
  /**
   * Store result
   *
   * @param project
   * @param result
   * @return
   * @throws StoreResultException
   */
  Boolean store(AbstractProject project, Object result) throws StoreResultException;

  /**
   * Load all result
   *
   * @param project
   * @return
   * @throws StoreResultException
   */
  Map<Integer, SubmittedResult> fetchAll(AbstractProject project)
    throws StoreResultException;
}
