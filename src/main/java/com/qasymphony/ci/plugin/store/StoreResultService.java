package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.model.SubmittedResult;
import hudson.model.AbstractProject;
import hudson.model.Job;

/**
 * @author trongle
 * @version 10/22/2015 9:20 PM trongle $
 * @since 1.0
 */
public interface StoreResultService {
  /**
   * Store result
   *
   * @param job job
   * @param result  result
   * @return true if save success
   * @throws StoreResultException StoreResultException
   */
  Boolean store(Job job, final SubmittedResult result) throws StoreResultException;

  /**
   * Load all result
   *
   * @param request request
   * @return {@link ReadSubmitLogRequest}
   * @throws StoreResultException StoreResultException
   */
  ReadSubmitLogResult fetchAll(ReadSubmitLogRequest request)
    throws StoreResultException;

  /**
   * Fetch with paging and search from file.
   *
   * @param request request
   * @return {@link ReadSubmitLogRequest}
   * @throws StoreResultException StoreResultException
   */
  ReadSubmitLogResult fetch(ReadSubmitLogRequest request)
    throws StoreResultException;
}
