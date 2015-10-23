package com.qasymphony.ci.plugin.submitter;

import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;

/**
 * @author trongle
 * @version 10/21/2015 2:09 PM trongle $
 * @since 1.0
 */
public class JunitQtestSubmitterImpl implements JunitSubmitter {
  @Override public JunitSubmitterResult submit(JunitSubmitterRequest junitSubmitterRequest) {
    //TODO: submit to qTest and receive test suite id
    JunitSubmitterResult junitSubmitterResult = new JunitSubmitterResult();
    junitSubmitterResult.setTestSuiteId(System.currentTimeMillis());
    return junitSubmitterResult;
  }

  @Override public void storeSubmitterResult() {

  }
}
