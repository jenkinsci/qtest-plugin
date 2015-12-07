package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import hudson.model.AbstractProject;

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
  Boolean store(AbstractProject project, final SubmittedResult result) throws StoreResultException;

  /**
   * Load all result
   *
   * @param request
   * @return
   * @throws StoreResultException
   */
  ReadSubmitLogResult fetchAll(ReadSubmitLogRequest request)
    throws StoreResultException;

  /**
   * Fetch with paging and search from file.
   *
   * @param request
   * @return
   * @throws StoreResultException
   */
  ReadSubmitLogResult fetch(ReadSubmitLogRequest request)
    throws StoreResultException;
}
