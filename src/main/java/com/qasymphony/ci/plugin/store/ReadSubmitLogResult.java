package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.model.SubmittedResult;

import java.util.Map;

/**
 * @author trongle
 * @version 12/4/2015 5:06 PM trongle $
 * @since 1.0
 */
public class ReadSubmitLogResult {
  private long total;
  private Map<Integer, SubmittedResult> results;

  public long getTotal() {
    return total;
  }

  public ReadSubmitLogResult setTotal(long total) {
    this.total = total;
    return this;
  }

  public Map<Integer, SubmittedResult> getResults() {
    return results;
  }

  public ReadSubmitLogResult setResults(Map<Integer, SubmittedResult> results) {
    this.results = results;
    return this;
  }
}
