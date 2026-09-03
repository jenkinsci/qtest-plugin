package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.AutomationTestService;
import com.qasymphony.ci.plugin.exception.OAuthException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.Configuration;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.support.QTestMockServerRule;
import com.qasymphony.ci.plugin.support.SubmitTestFixtures;
import com.qasymphony.ci.plugin.utils.ResponseEntity;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AutomationTestServiceErrorTests extends TestAbstracts {

  @Rule public QTestMockServerRule qtest = new QTestMockServerRule();

  @Test
  public void emptyResultsReturnsNull() throws SubmittedException {
    Configuration configuration = SubmitTestFixtures.ciConfiguration(qtest);
    JunitSubmitterRequest request = configuration.createJunitSubmitRequest();
    ResponseEntity response = AutomationTestService.push(
      "1", "/jobs/x/1", Collections.emptyList(), request, configuration.getAppSecretKey());
    assertNull(response);
  }

  @Test(expected = OAuthException.class)
  public void oauthFailureThrowsOAuthException() throws OAuthException {
    qtest.getServer().resetStubs();
    qtest.getServer().stubOAuthFailure(401);
    Configuration configuration = SubmitTestFixtures.ciConfiguration(qtest);
    SubmitTestFixtures.oauthAccessToken(qtest, configuration);
  }

  @Test
  public void submit4xxThrowsSubmittedException() throws Exception {
    qtest.getServer().resetStubs();
    qtest.getServer().stubOAuth().stubCiSubmitFailure(400);
    Configuration configuration = SubmitTestFixtures.ciConfiguration(qtest);
    JunitSubmitterRequest request = configuration.createJunitSubmitRequest();
    request.setBuildNumber("1").setBuildPath("/jobs/x/1");
    request.setTestResults(SubmitTestFixtures.singlePassedResult("submit-4xx-test"));

    JunitQtestSubmitterImpl submitter = new JunitQtestSubmitterImpl();
    try {
      submitter.submit(request);
      throw new AssertionError("Expected SubmittedException");
    } catch (SubmittedException e) {
      assertEquals(400, e.getStatus());
    }
  }
}
