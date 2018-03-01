package com.qasymphony.ci.plugin.action;

import com.google.common.collect.ImmutableSet;
import com.qasymphony.ci.plugin.*;
import com.qasymphony.ci.plugin.exception.StoreResultException;
import com.qasymphony.ci.plugin.exception.SubmittedException;
import com.qasymphony.ci.plugin.model.AutomationTestResult;
import com.qasymphony.ci.plugin.model.PipelineConfiguration;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.parse.JunitTestResultParser;
import com.qasymphony.ci.plugin.parse.ParseRequest;
import com.qasymphony.ci.plugin.submitter.JunitQtestSubmitterImpl;
import com.qasymphony.ci.plugin.submitter.JunitSubmitter;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterResult;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import com.qasymphony.ci.plugin.utils.JsonUtils;
import com.qasymphony.ci.plugin.utils.LoggerUtils;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SubmitJUnitStep extends Step {
    private static final Logger LOG = Logger.getLogger(PushingResultAction.class.getName());

    public PipelineConfiguration getPipeConfiguration() {
        return pipelineConfiguration;
    }

    public void setPipeConfiguration(PipelineConfiguration pipeConfiguration) {
        this.pipelineConfiguration = pipeConfiguration;
    }

    private PipelineConfiguration pipelineConfiguration;
    @DataBoundConstructor
    public  SubmitJUnitStep(PipelineConfiguration pipeConfiguration){
        this.pipelineConfiguration = pipeConfiguration;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new SubmitJUnitStepExecution(this, stepContext);
    }


    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "submitJUnitTestResultsToqTest";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class);
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Submit jUnit test result to qTest";
        }

//        @Override public String argumentsToString(Map<String, Object> namedArgs) {
//            Object name = namedArgs.get("name");
//            return name instanceof String ? (String) name : null;
//        }

        @Override
        public Step newInstance(@CheckForNull StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
            Boolean createTestSuiteEveryBuildDate = false;
            formData.remove("stapler-class");
            formData.remove("$class");

            PipelineConfiguration pipeConfig =  req.bindJSON(PipelineConfiguration.class, formData);
            pipeConfig.setQtestURL(formData.optString("url"));
            pipeConfig.setApiKey(formData.optString("appSecretKey"));
            pipeConfig.setProjectID(formData.optLong("projectId"));
            pipeConfig.setEnvironmentID(formData.optLong("environmentId"));
            pipeConfig.setCreateTestCaseForEachJUnitTestClass(!formData.optBoolean("eachMethodAsTestCase"));
            pipeConfig.setOverwriteExistingTestSteps(formData.optBoolean("overwriteExistingTestSteps"));
            pipeConfig.setSubmitToExistingContainer(formData.optBoolean("submitToContainer"));
            pipeConfig.setParseTestResultsFromTestingTools(formData.optBoolean("readFromJenkins"));
            pipeConfig.setParseTestResultsPattern(formData.optString("resultPattern"));
            Long containerID = formData.optLong("releaseId", 0L);
            String containerType = "release";
            if (pipeConfig.getSubmitToExistingContainer()) {
                JSONObject json = JSONObject.fromObject(formData.optString("containerSetting", "{}"));
                JSONObject selectedContainer = json.getJSONObject("selectedContainer");
                if (selectedContainer.has("dailyCreateTestSuite")) {
                    createTestSuiteEveryBuildDate = selectedContainer.optBoolean("dailyCreateTestSuite");
                }
                JSONArray containerPath = JSONArray.fromObject(json.getString("containerPath"));
                if (0 < containerPath.size()) {
                    JSONObject jsonContainer = containerPath.getJSONObject(containerPath.size() - 1);
                    if (null != jsonContainer) {
                        containerType = jsonContainer.getString("nodeType");
                        containerID = jsonContainer.optLong("nodeId", 0L);
                    }
                }
                pipeConfig.setCreateNewTestRunsEveryBuildDate(createTestSuiteEveryBuildDate);
            } else {
                // remove un-needed param
                pipeConfig.setCreateNewTestRunsEveryBuildDate(null);
            }

            pipeConfig.setContainerID(containerID);
            pipeConfig.setContainerType(containerType);
//            if (!pipeConfig.isValidate()) {
//                throw new Exception("Invalid configuration for pipeline");
//            }
            SubmitJUnitStep step = new SubmitJUnitStep(pipeConfig);
            return step;


        }

        // javascript methods

        /**
         * @param qTestUrl qtest url
         * @param apiKey   api key
         * @return a list of project
         */
        @JavaScriptMethod
        public JSONObject getProjects(String qTestUrl, String apiKey) {
            JSONObject res = new JSONObject();
            //get project from qTest
            Object projects = ConfigService.getProjects(qTestUrl, apiKey);
            res.put("projects", null == projects ? "" : JSONArray.fromObject(projects));
            return res;
        }

        /**
         * @param qTestUrl           qTestUrl
         * @param apiKey             apiKey
         * @param projectId          projectId
         * @param jenkinsProjectName jenkinsProjectName
         * @return data
         */
        @JavaScriptMethod
        public JSONObject getProjectData(final String qTestUrl, final String apiKey, final Long projectId, final String jenkinsProjectName) {

            final StaplerRequest request = Stapler.getCurrentRequest();
            final String jenkinsServerName = HttpClientUtils.getServerUrl(request);
            return qTestService.getProjectData(qTestUrl, apiKey, projectId, jenkinsProjectName, jenkinsServerName);
        }

        @JavaScriptMethod
        public JSONObject getContainerChildren(final  String qTestUrl, final String apiKey, final Long projectId, final Long parentId, final String parentType) {
            return qTestService.getContainerChildren(qTestUrl, apiKey, projectId, parentId, parentType);
        }

        @JavaScriptMethod
        public JSONObject getQtestInfo(String qTestUrl) {
            return qTestService.getQtestInfo(qTestUrl);
        }
        //~javascript methods
    }

    public static class SubmitJUnitStepExecution extends SynchronousNonBlockingStepExecution<Void> {
        private static final Logger LOG = Logger.getLogger(SubmitJUnitStepExecution.class.getName());

        private transient TaskListener listener;

        private transient FilePath ws;

        private transient Run build;

        private transient Launcher launcher;

        private transient final SubmitJUnitStep step;

        SubmitJUnitStepExecution(SubmitJUnitStep step, StepContext context) {
            super(context);
            this.step = step;
        }
        @Override
        protected Void run() throws Exception {

            this.build = getContext().get(Run.class);
            this.ws = getContext().get(FilePath.class);
            this.listener = getContext().get(TaskListener.class);
            this.launcher = getContext().get(Launcher.class);
            JunitSubmitterRequest junitSubmitterRequest = step.pipelineConfiguration.createJunitSubmitRequest();
            junitSubmitterRequest
                    .setBuildNumber(build.getNumber() + "")
                    .setBuildPath(build.getUrl())
                    .setJenkinsProjectName(ws.getBaseName()/*build.getParent().getDisplayName()*/)
                    .setJenkinsServerURL(Jenkins.getInstance().getRootUrl())
                    .setListener(listener);

            RunWrapper runWrapper = new RunWrapper (this.build, true);
            PrintStream logger = listener.getLogger();
            //LoggerUtils.formatInfo(logger, "Previous build status 1: " + runWrapper.getCurrentResult());
            JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
            if (Result.ABORTED.equals(runWrapper.getCurrentResult())) {
                LoggerUtils.formatWarn(logger, "Abort build action.");
                storeWhenNotSuccess(junitSubmitterRequest, junitSubmitter, build, runWrapper.getCurrentResult(),  logger, JunitSubmitterResult.STATUS_CANCELED);
                return null;
            }
            JSONObject infoObject = null;
            try {
                infoObject = this.loadPipelineConfiguration(junitSubmitterRequest, junitSubmitterRequest.getJenkinsProjectName(), junitSubmitterRequest.getJenkinsServerURL());
            } catch (Exception ex) {
                storeWhenNotSuccess(junitSubmitterRequest, junitSubmitter, build, runWrapper.getCurrentResult(), logger, JunitSubmitterResult.STATUS_FAILED);
                throw new Exception(ex);
            }

            if (null == infoObject) {
                storeWhenNotSuccess(junitSubmitterRequest, junitSubmitter, build, runWrapper.getCurrentResult(), logger, JunitSubmitterResult.STATUS_FAILED);
                return null;
            }

            showInfo(logger, infoObject);
            if (!step.pipelineConfiguration.isValidate()) {
                LoggerUtils.formatWarn(logger, "Invalid configuration to qTest, reject submit test results.");
                storeWhenNotSuccess(junitSubmitterRequest, junitSubmitter, build, runWrapper.getCurrentResult(), logger, JunitSubmitterResult.STATUS_FAILED);
                return null;
            }

            List<AutomationTestResult> automationTestResults = readTestResults(logger);
            if (automationTestResults.isEmpty()) {
                LoggerUtils.formatWarn(logger, "No JUnit test result found.");
                storeWhenNotSuccess(junitSubmitterRequest, junitSubmitter, build, runWrapper.getCurrentResult(), logger, JunitSubmitterResult.STATUS_SKIPPED);
                LoggerUtils.formatHR(logger);
                return null;
            }
            junitSubmitterRequest.setTestResults(automationTestResults);

            JunitSubmitterResult result = submitTestResult(junitSubmitterRequest, junitSubmitter, automationTestResults);
            if (null == result) {
                //if have no test result, we do not break build flow
                return null;
            }
            storeResult(junitSubmitterRequest, build, runWrapper.getCurrentResult(),junitSubmitter, result, logger);
            LoggerUtils.formatHR(logger);
            return null;

        }

        private void storeResult(JunitSubmitterRequest junitSubmitterRequest, Run<?, ?> build, String buildResult, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
            try {
                junitSubmitter.storeSubmittedResult(junitSubmitterRequest, build, buildResult, result);
                LoggerUtils.formatInfo(logger, "Store submission result to workspace success.");
            } catch (Exception e) {
                LoggerUtils.formatError(logger, "Cannot store submission result: " + e.getMessage());
                e.printStackTrace(logger);
            }
            LoggerUtils.formatInfo(logger, "");
        }

        private Boolean storeWhenNotSuccess(JunitSubmitterRequest submitterRequest, JunitSubmitter junitSubmitter, Run build, String buildStatus, PrintStream logger, String status) {
            try {
                junitSubmitter.storeSubmittedResult(submitterRequest, build, buildStatus, new JunitSubmitterResult()
                        .setNumberOfTestLog(0)
                        .setTestSuiteName("")
                        .setNumberOfTestResult(0)
                        .setTestSuiteId(null)
                        .setSubmittedStatus(status));
            } catch (StoreResultException e) {
                LoggerUtils.formatError(logger, e.getMessage());
                e.printStackTrace(logger);
            }
            return true;
        }

        private List<AutomationTestResult> readTestResults(PrintStream logger) {
            List<AutomationTestResult> automationTestResults;
            long start = System.currentTimeMillis();
            LoggerUtils.formatHR(logger);
            try {
                automationTestResults = JunitTestResultParser.parse(new ParseRequest()
                        .setBuild(build)
                        .setWorkSpace(ws)
                        .setLauncher(launcher)
                        .setListener(listener)
                        .setCreateEachMethodAsTestCase(!step.pipelineConfiguration.getCreateTestCaseForEachJUnitTestClass())
                        .setOverwriteExistingTestSteps(step.pipelineConfiguration.getOverwriteExistingTestSteps())
                        .setUtilizeTestResultFromCITool(step.pipelineConfiguration.getParseTestResultsFromTestingTools())
                        .setParseTestResultPattern(step.pipelineConfiguration.getParseTestResultsPattern())
                );
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage());
                LoggerUtils.formatError(logger, e.getMessage());
                automationTestResults = Collections.emptyList();
            }
            if (automationTestResults.isEmpty()) {
                return Collections.emptyList();
            }
            LoggerUtils.formatInfo(logger, "JUnit test result found: %s, time elapsed: %s", automationTestResults.size(), LoggerUtils.elapsedTime(start));
            LoggerUtils.formatHR(logger);
            LoggerUtils.formatInfo(logger, "");
            return automationTestResults;
        }

        private JunitSubmitterResult submitTestResult(JunitSubmitterRequest request,
                                                      JunitSubmitter junitSubmitter, List<AutomationTestResult> automationTestResults) {
            PrintStream logger = listener.getLogger();
            JunitSubmitterResult result = null;
            LoggerUtils.formatInfo(logger, "Begin submit test results to qTest at: " + JsonUtils.getCurrentDateString());
            long start = System.currentTimeMillis();
            try {
                result = junitSubmitter.submit(request);
            } catch (SubmittedException e) {
                LoggerUtils.formatError(logger, "Cannot submit test results to qTest:");
                LoggerUtils.formatError(logger, "   status code: " + e.getStatus());
                LoggerUtils.formatError(logger, "   error: " + e.getMessage());
            } catch (Exception e) {
                LoggerUtils.formatError(logger, "Cannot submit test results to qTest:");
                LoggerUtils.formatError(logger, "   error: " + e.getMessage());
            } finally {
                if (null == result) {
                    result = new JunitSubmitterResult()
                            .setTestSuiteId(null)
                            .setSubmittedStatus(JunitSubmitterResult.STATUS_FAILED)
                            .setNumberOfTestResult(automationTestResults.size())
                            .setNumberOfTestLog(0);
                }

                Boolean isSuccess = null != result.getTestSuiteId() && result.getTestSuiteId() > 0;
                LoggerUtils.formatHR(logger);
                LoggerUtils.formatInfo(logger, isSuccess ? "SUBMIT SUCCESS" : "SUBMIT FAILED");
                LoggerUtils.formatHR(logger);
                if (isSuccess) {
                    int numberTestLog = 0 != result.getNumberOfTestLog() ? result.getNumberOfTestLog() : automationTestResults.size();
                    LoggerUtils.formatInfo(logger, "   testLogs: %s", numberTestLog);
                    LoggerUtils.formatInfo(logger, "   testSuite: name=%s, id=%s", result.getTestSuiteName(), result.getTestSuiteId());
                    LoggerUtils.formatInfo(logger, "   link: %s", ConfigService.formatTestSuiteLink(step.pipelineConfiguration.getQtestURL(), step.pipelineConfiguration.getProjectID(), result.getTestSuiteId()));
                }
                LoggerUtils.formatInfo(logger, "Time elapsed: %s", LoggerUtils.elapsedTime(start));
                LoggerUtils.formatInfo(logger, "End submit test results to qTest at: %s", JsonUtils.getCurrentDateString());
                LoggerUtils.formatInfo(logger, "");
            }
            return result;
        }

        private JSONObject loadPipelineConfiguration(JunitSubmitterRequest junitSubmitterRequest, String jenkinsProjectName, String jenkinsServerURL) throws Exception {
            //ConfigService.saveConfiguration()
            // get config from qTest
            // save config if not saved before
            // get some info from qTest, EX: container name, environment name, parent environment id, release name, config id, module id
            PipelineConfiguration pipelineConfiguration = this.step.pipelineConfiguration;
            Boolean saveOldSetting;
            saveOldSetting = ConfigService.compareqTestVersion(pipelineConfiguration.getQtestURL(), Constants.OLD_QTEST_VERSION);
            Setting settingFromConfig = pipelineConfiguration.toSetting(saveOldSetting, jenkinsServerURL, jenkinsProjectName);

            Setting setting = ConfigService.saveConfiguration(pipelineConfiguration.getQtestURL(), pipelineConfiguration.getApiKey(), settingFromConfig);
            if (null != setting) {
                junitSubmitterRequest.setModuleID(setting.getModuleId());
                junitSubmitterRequest.setConfigurationID(setting.getId());
                //junitSubmitterRequest.setEnvironmentParentID()
                String accessToken = OauthProvider.getAccessToken(pipelineConfiguration.getQtestURL(), pipelineConfiguration.getApiKey());
                Map<String, String> headers = OauthProvider.buildHeaders(accessToken, null);
                // get project name
                JSONObject projectInfo = qTestService.getProjectInfo(pipelineConfiguration.getQtestURL(), headers, pipelineConfiguration.getProjectID());

                // get container name
                JSONObject containerInfo = qTestService.getContainerInfo(pipelineConfiguration.getQtestURL(), headers, pipelineConfiguration.getProjectID(), pipelineConfiguration.getContainerType(), pipelineConfiguration.getContainerID());

                JSONObject jsonObject = new JSONObject();
                if (pipelineConfiguration.getEnvironmentID() > 0L) {
                    Object environments = ConfigService.getEnvironments(pipelineConfiguration.getQtestURL(), accessToken, pipelineConfiguration.getProjectID());
                    JSONObject environmentJSON = JSONObject.fromObject(environments);
                    JSONArray environmentList = environmentJSON.optJSONArray("allowed_values");
                    if (null != environmentList) {
                        for (int i = 0; i < environmentList.size(); i++) {
                            if(environmentList.getJSONObject(i).optLong("value") == pipelineConfiguration.getEnvironmentID()) {
                                junitSubmitterRequest.setEnvironmentParentID(environmentJSON.optLong("id"));
                                jsonObject.put(Constants.ENVIRONMENT_NAME, environmentList.getJSONObject(i).optString("label"));
                                break;
                            }
                        }
                    }
                }
                jsonObject.put(Constants.PROJECT_NAME, projectInfo.optString("name"));
                jsonObject.put(Constants.CONTAINER_NAME, containerInfo.optString("name"));
                jsonObject.put(Constants.CONFIGURATION_ID, setting.getId());

                return jsonObject;
            }

            return null;
        }

        private void showInfo(PrintStream logger, JSONObject jsonObject) {
            PipelineConfiguration pipelineConfiguration = this.step.pipelineConfiguration;
            LoggerUtils.formatInfo(logger, "");
            LoggerUtils.formatHR(logger);
            LoggerUtils.formatInfo(logger, ResourceBundle.DISPLAY_NAME);
            LoggerUtils.formatInfo(logger, String.format("Build Version: %s", ConfigService.getBuildVersion()));
            LoggerUtils.formatHR(logger);
            LoggerUtils.formatInfo(logger, "Submit Junit test result to qTest at:%s (cid:%s)", pipelineConfiguration.getQtestURL(), jsonObject.optString(Constants.CONFIGURATION_ID));
            LoggerUtils.formatInfo(logger, "With project: %s (id=%s).", jsonObject.optString(Constants.PROJECT_NAME), pipelineConfiguration.getProjectID());
            if (!pipelineConfiguration.getSubmitToExistingContainer()) {
                LoggerUtils.formatInfo(logger, "With release: %s (id=%s).", jsonObject.optString(Constants.CONTAINER_NAME), pipelineConfiguration.getContainerID());
            } else {
                LoggerUtils.formatInfo(logger, "With container: %s (id=%s, type=%s).",
                        jsonObject.optString(Constants.CONTAINER_NAME),
                        pipelineConfiguration.getContainerID(), pipelineConfiguration.getContainerType());
            }
            Long environmentID = pipelineConfiguration.getEnvironmentID();
            if (null != environmentID && 0 < environmentID) {
                LoggerUtils.formatInfo(logger, "With environment: %s (id=%s).", jsonObject.optString(Constants.ENVIRONMENT_NAME), environmentID);
            } else {
                LoggerUtils.formatInfo(logger, "With no environment.");
            }
            LoggerUtils.formatInfo(logger, "");
        }
    }

}

