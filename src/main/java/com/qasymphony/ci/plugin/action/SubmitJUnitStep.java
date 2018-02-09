package com.qasymphony.ci.plugin.action;

import com.google.common.collect.ImmutableSet;
import com.qasymphony.ci.plugin.ConfigService;
import com.qasymphony.ci.plugin.OauthProvider;
import com.qasymphony.ci.plugin.ResourceBundle;
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
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
//            formData.remove("stapler-class");
//            formData.remove("$class");

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
            final JSONObject res = new JSONObject();
            final StaplerRequest request = Stapler.getCurrentRequest();
            final String jenkinsServerName = HttpClientUtils.getServerUrl(request);
            String token = null;
            try {
                token = OauthProvider.getAccessToken(qTestUrl, apiKey);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error while get projectData:" + e.getMessage());
            }
            final String accessToken = token;

            Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
            if (null == project) {
                //if project not found, we return empty data
                res.put("setting", "");
                res.put("releases", "");
                res.put("environments", "");
                res.put("testCycles", "");
                res.put("testSuites", "");
                return res;
            }
            final int threadCount = 5;
            final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
            ExecutorService fixedPool = Executors.newFixedThreadPool(threadCount);
            Callable<Object> caGetSetting = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        //get saved setting from qtest
                        Object setting = ConfigService.getConfiguration(new Setting().setJenkinsServer(jenkinsServerName)
                                        .setJenkinsProjectName(jenkinsProjectName)
                                        .setProjectId(projectId)
                                        .setServerId(ConfigService.getServerId(jenkinsServerName)),
                                qTestUrl, accessToken);
                        res.put("setting", null == setting ? "" : JSONObject.fromObject(setting));
                        return setting;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            Callable<Object> caGetReleases = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object releases = ConfigService.getReleases(qTestUrl, accessToken, projectId);
                        res.put("releases", null == releases ? "" : JSONArray.fromObject(releases));
                        return releases;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            Callable<Object> caGetTestCycles = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object testCycles = ConfigService.getTestCycleChildren(qTestUrl, accessToken, projectId, (long) 0, "root");
                        res.put("testCycles", null == testCycles ? "" : JSONArray.fromObject(testCycles));
                        return testCycles;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            Callable<Object> caGetTestSuites = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object testSuites = ConfigService.getTestSuiteChildren(qTestUrl, accessToken, projectId, (long) 0, "root");
                        res.put("testSuites", null == testSuites ? "" : JSONArray.fromObject(testSuites));
                        return testSuites;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            Callable<Object> caGetEnvs = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object environments = ConfigService.getEnvironments(qTestUrl, accessToken, projectId);
                        res.put("environments", null == environments ? "" : environments);
                        return environments;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            fixedPool.submit(caGetSetting);
            fixedPool.submit(caGetReleases);
            fixedPool.submit(caGetEnvs);
            fixedPool.submit(caGetTestCycles);
            fixedPool.submit(caGetTestSuites);

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, e.getMessage());
            } finally {
                fixedPool.shutdownNow();
                return res;
            }
        }

        @JavaScriptMethod
        public JSONObject getContainerChildren(final  String qTestUrl, final String apiKey, final Long projectId, final Long parentId, final String parentType) {
            final JSONObject res = new JSONObject();
            String token = null;
            try {
                token = OauthProvider.getAccessToken(qTestUrl, apiKey);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error while get projectData:" + e.getMessage());
            }
            final String accessToken = token;

            Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
            if (null == project) {
                //if project not found, we return empty data
                res.put("testCycles", "");
                res.put("testSuites", "");
                return res;
            }
            final int threadCount = 2;
            final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
            ExecutorService fixedPool = Executors.newFixedThreadPool(threadCount);
            Callable<Object> caGetTestCycles = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object testCycles = ConfigService.getTestCycleChildren(qTestUrl, accessToken, projectId, parentId, parentType);
                        res.put("testCycles", null == testCycles ? "" : JSONArray.fromObject(testCycles));
                        return testCycles;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            Callable<Object> caGetTestSuites = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Object testSuites = ConfigService.getTestSuiteChildren(qTestUrl, accessToken, projectId, parentId, parentType);
                        res.put("testSuites", null == testSuites ? "" : JSONArray.fromObject(testSuites));
                        return testSuites;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
            fixedPool.submit(caGetTestCycles);
            fixedPool.submit(caGetTestSuites);

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, e.getMessage());
            } finally {
                fixedPool.shutdownNow();
                return res;
            }
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

            //RunWrapper runWrapper = new RunWrapper (this.build, true);

            PrintStream logger = listener.getLogger();
            //LoggerUtils.formatInfo(logger, "Previous build status 1: " + runWrapper.getResult());
            //LoggerUtils.formatInfo(logger, "Previous build status 2: " + build.getResult());

//            Result ret = build.getResult();
//            LoggerUtils.formatInfo(logger, "Build result " + ret.toString());
            JunitSubmitter junitSubmitter = new JunitQtestSubmitterImpl();
//            if (Result.ABORTED.equals(build.getResult())) {
//                LoggerUtils.formatWarn(logger, "Abort build action.");
//                storeWhenNotSuccess(junitSubmitter, build, logger, JunitSubmitterResult.STATUS_CANCELED);
//                return null;
//            }
            showInfo(logger);
            if (!step.pipelineConfiguration.isValidate()) {
                LoggerUtils.formatWarn(logger, "Invalid configuration to qTest, reject submit test results.");
                storeWhenNotSuccess(junitSubmitterRequest, junitSubmitter, build, logger, JunitSubmitterResult.STATUS_FAILED);
                return null;
            }

            List<AutomationTestResult> automationTestResults = readTestResults(junitSubmitterRequest, logger, junitSubmitter);
            if (automationTestResults.isEmpty()) {
                return null;
            }
            junitSubmitterRequest.setTestResults(automationTestResults);


            JunitSubmitterResult result = submitTestResult(junitSubmitterRequest, junitSubmitter, automationTestResults);
            if (null == result) {
                //if have no test result, we do not break build flow
                return null;
            }
            storeResult(junitSubmitterRequest, build, junitSubmitter, result, logger);
            LoggerUtils.formatHR(logger);
            return null;

        }

        private void storeResult(JunitSubmitterRequest junitSubmitterRequest, Run<?, ?> build, JunitSubmitter junitSubmitter, JunitSubmitterResult result, PrintStream logger) {
            try {
                junitSubmitter.storeSubmittedResult(junitSubmitterRequest, build, result);
                LoggerUtils.formatInfo(logger, "Store submission result to workspace success.");
            } catch (Exception e) {
                LoggerUtils.formatError(logger, "Cannot store submission result: " + e.getMessage());
                e.printStackTrace(logger);
            }
            LoggerUtils.formatInfo(logger, "");
        }

        private Boolean storeWhenNotSuccess(JunitSubmitterRequest submitterRequest, JunitSubmitter junitSubmitter, Run build, PrintStream logger, String status) {
            try {
                junitSubmitter.storeSubmittedResult(submitterRequest, build, new JunitSubmitterResult()
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

        private List<AutomationTestResult> readTestResults(JunitSubmitterRequest submitterRequest, PrintStream logger, JunitSubmitter junitSubmitter) {
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
                LoggerUtils.formatWarn(logger, "No JUnit test result found.");
                storeWhenNotSuccess(submitterRequest, junitSubmitter, build, logger, JunitSubmitterResult.STATUS_SKIPPED);
                LoggerUtils.formatHR(logger);
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

        private boolean loadPipelineConfiguration() {
            //ConfigService.saveConfiguration()
            //this.build.getUpstreamBuilds

            return false;
        }

        private void showInfo(PrintStream logger) {
            PipelineConfiguration pipelineConfiguration = this.step.pipelineConfiguration;
            LoggerUtils.formatInfo(logger, "");
            LoggerUtils.formatHR(logger);
            LoggerUtils.formatInfo(logger, ResourceBundle.DISPLAY_NAME);
            LoggerUtils.formatInfo(logger, String.format("Build Version: %s", ConfigService.getBuildVersion()));
            LoggerUtils.formatHR(logger);
            LoggerUtils.formatInfo(logger, "Submit Junit test result to qTest at:%s (cid:%s)", pipelineConfiguration.getQtestURL(), "TODO GET CONFIG ID");
            LoggerUtils.formatInfo(logger, "With project: %s (id=%s).", ws.getBaseName(), pipelineConfiguration.getProjectID());
            if (!pipelineConfiguration.getSubmitToExistingContainer()) {
                LoggerUtils.formatInfo(logger, "With release: %s (id=%s).", "TODO GET RELEASE NAME", pipelineConfiguration.getContainerID());
            } else {
                LoggerUtils.formatInfo(logger, "With container: %s (id=%s, type=%s).",
                        "TODO GET CONTAINER",
                        pipelineConfiguration.getContainerID(), pipelineConfiguration.getContainerType());
            }
            Long environmentID = pipelineConfiguration.getEnvironmentID();
            if (null != environmentID && 0 < environmentID) {
                LoggerUtils.formatInfo(logger, "With environment: %s (id=%s).", "TODO NEED READ ENVIRONMENT NAME", environmentID);
            } else {
                LoggerUtils.formatInfo(logger, "With no environment.");
            }
            LoggerUtils.formatInfo(logger, "");
        }
    }

}

