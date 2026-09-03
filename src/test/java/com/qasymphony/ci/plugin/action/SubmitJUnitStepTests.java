package com.qasymphony.ci.plugin.action;

import com.qasymphony.ci.plugin.store.TestAbstracts;
import com.qasymphony.ci.plugin.support.QTestMockServerRule;
import jenkins.model.JenkinsLocationConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import static org.junit.Assert.assertNotNull;

public class SubmitJUnitStepTests extends TestAbstracts {

  @Rule public QTestMockServerRule qtest = new QTestMockServerRule();

  @Test
  public void pipelineStepSubmitsParsedJUnitResults() throws Exception {
    qtest.getServer().stubPipelineSupport();

    JenkinsLocationConfiguration.get().setUrl("http://localhost:8080/jenkins/");

    String baseUrl = qtest.baseUrl();
    String junitXml = "<testsuite name=\\\"demo.DemoTest\\\" tests=\\\"1\\\" failures=\\\"0\\\" "
      + "errors=\\\"0\\\" skipped=\\\"0\\\">"
      + "<testcase name=\\\"testOk\\\" classname=\\\"demo.DemoTest\\\" time=\\\"0.001\\\"/>"
      + "</testsuite>";
    String pipeline = ""
      + "node {\n"
      + "  sh 'mkdir -p build/test && "
      + "echo \"" + junitXml + "\" > build/test/TEST-demo.xml'\n"
      + "  submitJUnitTestResultsToqTest(\n"
      + "    qtestURL: '" + baseUrl + "',\n"
      + "    apiKey: '3c76feb4-b91f-4a53-8643-bd1ce2f01a3e',\n"
      + "    projectID: 1L,\n"
      + "    containerID: 1L,\n"
      + "    containerType: 'release',\n"
      + "    overwriteExistingTestSteps: false,\n"
      + "    parseTestResultsFromTestingTools: true,\n"
      + "    createTestCaseForEachJUnitTestClass: true,\n"
      + "    submitToExistingContainer: false,\n"
      + "    submitToAReleaseAsSettingFromQtest: true,\n"
      + "    utilizeTestResultsFromCITool: false,\n"
      + "    createTestCaseForEachJUnitTestMethod: false,\n"
      + "    parseTestResultsPattern: 'build/test/*.xml'\n"
      + "  )\n"
      + "}\n";

    WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "pipeline-qtest");
    job.setDefinition(new CpsFlowDefinition(pipeline, true));

    WorkflowRun run = j.buildAndAssertSuccess(job);
    assertNotNull(run);

    qtest.getServer().verifyOAuthCalledAtLeast(2);
    qtest.getServer().verifyCiSubmitCalledOnce();
  }
}
