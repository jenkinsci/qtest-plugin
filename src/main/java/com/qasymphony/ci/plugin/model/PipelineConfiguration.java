package com.qasymphony.ci.plugin.model;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class PipelineConfiguration extends AbstractDescribableImpl<PipelineConfiguration> {

    public static PipelineConfiguration newInstance() {
        return new PipelineConfiguration("", "", 0L, 0L, "", 0L, "", 0L,
                false, false, false,  false, false);
    }
    @DataBoundConstructor
    public PipelineConfiguration(String url,
            String appSecretKey,
            Long projectId,
            Long containerId,
            String containerType,
            Long environmentId,
            String resultPattern,
            Long moduleId,
            Boolean overwriteExistingTestSteps,
            Boolean createNewTestSuiteEveryBuild,
            Boolean readFromJenkins,
            Boolean eachMethodAsTestCase,
            Boolean submitToContainer) {
        this.url = url;
        this.appSecretKey = appSecretKey;
        this.projectId = projectId;
        this.containerId = containerId;
        this.containerType = containerType;
        this.environmentId = environmentId;
        this.resultPattern = resultPattern;
        this.moduleId = moduleId;
        this.overwriteExistingTestSteps = overwriteExistingTestSteps;
        this.createNewTestSuiteEveryBuild = createNewTestSuiteEveryBuild;
        this.readFromJenkins = readFromJenkins;
        this.eachMethodAsTestCase = eachMethodAsTestCase;
        this.submitToContainer = submitToContainer;
    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAppSecretKey() {
        return appSecretKey;
    }

    public void setAppSecretKey(String appSecretKey) {
        this.appSecretKey = appSecretKey;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getContainerId() {
        return containerId;
    }

    public void setContainerId(Long containerId) {
        this.containerId = containerId;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getResultPattern() {
        return resultPattern;
    }

    public void setResultPattern(String resultPattern) {
        this.resultPattern = resultPattern;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public Boolean isOverwriteExistingTestSteps() {
        return overwriteExistingTestSteps;
    }

    public void setOverwriteExistingTestSteps(Boolean overwriteExistingTestSteps) {
        this.overwriteExistingTestSteps = overwriteExistingTestSteps;
    }

    public Boolean isCreateNewTestSuiteEveryBuild() {
        return createNewTestSuiteEveryBuild;
    }

    public void setCreateNewTestSuiteEveryBuild(Boolean createNewTestSuiteEveryBuild) {
        this.createNewTestSuiteEveryBuild = createNewTestSuiteEveryBuild;
    }

    public Boolean isReadFromJenkins() {
        return readFromJenkins;
    }

    public void setReadFromJenkins(Boolean readFromJenkins) {
        this.readFromJenkins = readFromJenkins;
    }

    public Boolean isEachMethodAsTestCase() {
        return eachMethodAsTestCase;
    }

    public void setEachMethodAsTestCase(Boolean eachMethodAsTestCase) {
        this.eachMethodAsTestCase = eachMethodAsTestCase;
    }

    public Boolean isSubmitToContainer() {
        return submitToContainer;
    }

    public void setSubmitToContainer(boolean submitToContainer) {
        this.submitToContainer = submitToContainer;
    }

    public boolean isValidate() {
        boolean ret = StringUtils.isNotEmpty(this.url)
                && StringUtils.isNotEmpty(this.appSecretKey)
                && this.projectId > 0L
                && this.containerId > 0L
                && StringUtils.isNotEmpty(this.containerType);
//        if (!ret)   return ret;
//        if (submitToContainer) {
//
//        }
        return ret;
    }

    @Override
    public String toString() {
        return "{" +
                ", url='" + url + '\'' +
                ", appSecretKey='" + appSecretKey + '\'' +
                ", projectId=" + projectId +
                ", containerId=" + containerId +
                ", containerType=" + containerType +
                ", environmentId=" + environmentId +
                ", resultPattern=" + resultPattern +
                ", moduleId=" + moduleId +
                ", overwriteExistingTestSteps=" + overwriteExistingTestSteps +
                ", createNewTestSuiteEveryBuild=" + createNewTestSuiteEveryBuild +
                ", submitToContainer=" + submitToContainer +
                ", readFromJenkins=" + readFromJenkins +
                ", eachMethodAsTestCase='" + eachMethodAsTestCase + '\'' +
                '}';
    }


    protected String url;
    protected String appSecretKey;
    protected Long projectId;
    protected Long containerId;   // id of containerType
    protected String containerType; // release | test cycle | test suite
    protected Long environmentId;
    protected String resultPattern;
    protected Long moduleId; // module where test case created
    protected Boolean overwriteExistingTestSteps;
    protected Boolean createNewTestSuiteEveryBuild;
    protected Boolean readFromJenkins;
    protected Boolean eachMethodAsTestCase;
    protected Boolean submitToContainer;


    @Extension
    public static class DescriptorImp extends Descriptor<PipelineConfiguration> {
        public String getDisplayName() {
            return "";
        }
    }
}
