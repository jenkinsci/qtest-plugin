package com.qasymphony.ci.plugin.model;

import com.qasymphony.ci.plugin.model.qtest.Container;
import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.submitter.JunitSubmitterRequest;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class PipelineConfiguration extends AbstractDescribableImpl<PipelineConfiguration> {

    public static PipelineConfiguration newInstance() {
        return new PipelineConfiguration("", "", 0L, 0L, "", 0L, "",
                false, false,  false, false);
    }
    @DataBoundConstructor
    public PipelineConfiguration(String qtestURL,
            String apiKey,
            Long projectID,
            Long containerID,
            String containerType,
            Long environmentID,
            String parseTestResultsPattern,
            Boolean overwriteExistingTestSteps,
            Boolean parseTestResultsFromTestingTools,
            Boolean createTestCaseForEachJUnitTestClass,
            Boolean submitToExistingContainer) {
        this.qtestURL = qtestURL;
        this.apiKey = apiKey;
        this.projectID = projectID;
        this.containerID = containerID;
        this.containerType = containerType;
        this.environmentID = environmentID;
        this.parseTestResultsPattern = parseTestResultsPattern;
        this.moduleID = 0L;
        this.overwriteExistingTestSteps = overwriteExistingTestSteps;
        this.parseTestResultsFromTestingTools = parseTestResultsFromTestingTools;
        this.createTestCaseForEachJUnitTestClass = createTestCaseForEachJUnitTestClass;
        this.submitToExistingContainer = submitToExistingContainer;
    }

    private boolean validLong(Long l) {
        return null != l && l >= 0L;
    }


    public String getErrorString() {
        if (StringUtils.isEmpty(this.getQtestURL())) {
            return ("qtestURL must not be null");
        }
        if (StringUtils.isEmpty(this.getApiKey())) {
            return ("apiKey must not be null");
        }
        if (!validLong(this.getProjectID())) {
            return ("projectID must be null");
        }
        if (!validLong(this.getContainerID())) {
            return ("containerID must be null");
        }
        String containerType = this.getContainerType();
        if (StringUtils.isEmpty(containerType)) {
            return ("containerType must not be null");
        } else {
            containerType = containerType.toLowerCase();
            if (!(0 == containerType.compareToIgnoreCase("release") ||
                0 == containerType.compareToIgnoreCase("test-cycle") ||
                0 == containerType.compareToIgnoreCase("test-suite"))) {
                return ("containerType must be 'release' or 'test-suite' or 'test-cycle'");
            }
        }
        if (null == this.getSubmitToExistingContainer()) {
            return ("submitToExistingContainer parameter must not be null");
        }
        if (null == this.getCreateTestCaseForEachJUnitTestClass()) {
            return ("createTestCaseForEachJUnitTestClass parameter must not be null");
        }
        if (null == this.getParseTestResultsFromTestingTools()) {
            return ("parseTestResultsFromTestingTools parameter must not be null");
        }
        if (null == this.getParseTestResultsPattern()) {
            return ("parseTestResultsPattern parameter must not be null");
        }
        if (!validLong(this.getEnvironmentID())) {
            return ("environmentID parameter must not be null");
        }
        if (this.getSubmitToExistingContainer() && null == this.getCreateNewTestRunsEveryBuildDate()) {
            return ("createNewTestRunsEveryBuildDate parameter must not be null");
        }
        return null;
    }

    @Override
    public String toString() {
        return "{" +
                ", qtestURL='" + qtestURL + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", projectID=" + projectID +
                ", projectID=" + containerID +
                ", containerType= " + containerType +
                ", environmentID= " + environmentID+
                ", parseTestResultsPattern= " + parseTestResultsPattern +
                ", moduleID=" + moduleID +
                ", overwriteExistingTestSteps = " + overwriteExistingTestSteps +
                ", createNewTestRunsEveryBuildDate = " + createNewTestRunsEveryBuildDate +
                ", submitToExistingContainer = " + submitToExistingContainer +
                ", parseTestResultsFromTestingTools = " + parseTestResultsFromTestingTools +
                ", createTestCaseForEachJUnitTestClass='" + createTestCaseForEachJUnitTestClass + '\'' +
                '}';
    }


    protected String qtestURL;
    protected String apiKey;
    protected Long projectID;
    protected Long containerID;   // id of containerType
    protected String containerType; // release | test cycle | test suite
    protected Long environmentID;
    protected String parseTestResultsPattern;

    protected Long moduleID; // module where test case created
    protected Boolean overwriteExistingTestSteps;
    protected Boolean createNewTestRunsEveryBuildDate;
    protected Boolean parseTestResultsFromTestingTools;
    protected Boolean createTestCaseForEachJUnitTestClass;
    protected Boolean submitToExistingContainer;

    public String getQtestURL() {
        return qtestURL;
    }

    public void setQtestURL(String qtestURL) {
        this.qtestURL = qtestURL;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getProjectID() {
        return projectID;
    }

    public void setProjectID(Long projectID) {
        this.projectID = projectID;
    }

    public Long getContainerID() {
        return containerID;
    }

    public void setContainerID(Long containerID) {
        this.containerID = containerID;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
    }

    public Long getEnvironmentID() {
        return environmentID;
    }

    public void setEnvironmentID(Long environmentID) {
        this.environmentID = environmentID;
    }

    public String getParseTestResultsPattern() {
        return parseTestResultsPattern;
    }

    public void setParseTestResultsPattern(String parseTestResultsPattern) {
        this.parseTestResultsPattern = parseTestResultsPattern;
    }

    public Long getModuleID() {
        return moduleID;
    }

    public void setModuleID(Long moduleID) {
        this.moduleID = moduleID;
    }

    public Boolean getOverwriteExistingTestSteps() {
        return overwriteExistingTestSteps;
    }

    public void setOverwriteExistingTestSteps(Boolean overwriteExistingTestSteps) {
        this.overwriteExistingTestSteps = overwriteExistingTestSteps;
    }

    public Boolean getCreateNewTestRunsEveryBuildDate() {
        return createNewTestRunsEveryBuildDate;
    }

    @DataBoundSetter
    public void setCreateNewTestRunsEveryBuildDate(Boolean createNewTestRunsEveryBuildDate) {
        this.createNewTestRunsEveryBuildDate = createNewTestRunsEveryBuildDate;
    }

    public Boolean getParseTestResultsFromTestingTools() {
        return parseTestResultsFromTestingTools;
    }

    public void setParseTestResultsFromTestingTools(Boolean parseTestResultsFromTestingTools) {
        this.parseTestResultsFromTestingTools = parseTestResultsFromTestingTools;
    }

    public Boolean getCreateTestCaseForEachJUnitTestClass() {
        return createTestCaseForEachJUnitTestClass;
    }

    public void setCreateTestCaseForEachJUnitTestClass(Boolean createTestCaseForEachJUnitTestClass) {
        this.createTestCaseForEachJUnitTestClass = createTestCaseForEachJUnitTestClass;
    }

    public Boolean getSubmitToExistingContainer() {
        return submitToExistingContainer;
    }

    public void setSubmitToExistingContainer(Boolean submitToExistingContainer) {
        this.submitToExistingContainer = submitToExistingContainer;
    }

    public JunitSubmitterRequest createJunitSubmitRequest() {
        JunitSubmitterRequest request = new JunitSubmitterRequest();
        request.setqTestURL(this.qtestURL)
                .setApiKey(this.apiKey)
                .setConfigurationID(null)
                .setSubmitToExistingContainer(this.submitToExistingContainer)
                .setContainerID(containerID)
                .setContainerType(containerType)
                .setCreateNewTestRunsEveryBuildDate(submitToExistingContainer ? createNewTestRunsEveryBuildDate : null)
                .setEnvironmentID(this.environmentID)
                //.setEnvironmentParentID(this.en)  // get when build actually run
                //.setJenkinsProjectName(this.jenkinsProjectName) // get when build actually run
                //.setModuleID(this.moduleId) //get when build actually run
                //.setJenkinsServerURL(this.jenkinsServerUrl) // get when build actually run
                .setProjectID(this.projectID);
        return request;

    }
    /**
     * @param saveOldSetting work with old qTest version
     * @param jenkinsServerURL jenskins server url
     * @param jenkinsProjectName jenkins project name
     * @return {@link Setting}
     */
    public Setting toSetting(Boolean saveOldSetting, String jenkinsServerURL, String jenkinsProjectName) {

        Setting setting = new Setting()
                .setId(0L /*this.id*/)
                .setJenkinsServer(jenkinsServerURL /*this.jenkinsServerUrl*/)
                .setJenkinsProjectName(jenkinsProjectName /*this.jenkinsProjectName*/)
                .setProjectId(this.projectID)
                .setModuleId(0L /*this.moduleId*/)
                .setEnvironmentId(this.environmentID /*this.environmentId*/)
                .setTestSuiteId(0L /*this.testSuiteId*/);

        if (saveOldSetting == true) { // Save old setting for release option if qTest version < 8.9.4
            setting.setReleaseId(this.containerID);
            return setting;
        }

        setting.setOverwriteExistingTestSteps(this.overwriteExistingTestSteps);

        if (this.submitToExistingContainer) {
            setting.setContainer(this.getContainerInfo());
        } else {
            setting.setReleaseId(this.containerID);
        }
        return setting;
    }
    private Container getContainerInfo() {
        Container container = new Container();
        container.setId(this.containerID);
        container.setType(this.containerType.toLowerCase());
        container.setCreateNewTestSuiteEveryBuild(this.createNewTestRunsEveryBuildDate);
        return container;
    }

    @Extension
    public static class DescriptorImp extends Descriptor<PipelineConfiguration> {
        public String getDisplayName() {
            return "";
        }
    }
}
