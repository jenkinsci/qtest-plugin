package com.qasymphony.ci.plugin.model;

import com.qasymphony.ci.plugin.ResourceBundle;
import com.qasymphony.ci.plugin.utils.StreamWrapper;
import com.qasymphony.ci.plugin.utils.process.ProcessWrapper;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;

public class ToscaIntegration extends AbstractDescribableImpl<ToscaIntegration> implements ExternalTool {

    @DataBoundConstructor
    public ToscaIntegration(String command, String arguments, String resultPath) {
        this.resultPath = resultPath;
        this.command = command;
        this.arguments = arguments;
    }
    @Override
    public String getArguments() {
        return arguments;
    }

    @Override
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public String getCommand() {
        return this.command;
    }

    @Override
    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String validate() {
        if (StringUtils.isEmpty(this.command)) {
            return ResourceBundle.MSG_INVALID_EXTERNAL_COMMAND;
        }
        if (StringUtils.isEmpty(this.arguments)) {
            return ResourceBundle.MSG_INVALID_EXTERNAL_ARGUMENTS;
        }
        if (StringUtils.isEmpty(this.resultPath)) {
            return ResourceBundle.MSG_INVALID_EXTERNAL_RESULT_PATH;
        }
        return null;
    }

    @Override
    public String getResultPath() {
        return resultPath;
    }

    @Override
    public void setResultPath(String value) {
        this.resultPath = value;
    }

    @Override
    public int execute(Object... params) throws Exception {
        if (1 == params.length) {
            PrintStream logger = (PrintStream)params[0];
            ProcessWrapper processWrapper = new ProcessWrapper();
            StreamWrapper outStreamWrapper = new StreamWrapper(logger, false);
            StreamWrapper errStreamWrapper = new StreamWrapper(logger, true);
            processWrapper.createProcess(null, getCommand(), getArguments(),null, outStreamWrapper, errStreamWrapper);
            return processWrapper.waitForExit();
        }
        return -1;
    }

    private String arguments;
    private String command ;
    private String resultPath;

    @Symbol("executeToscaTests")
    @Extension
    public static class DescriptorImp extends Descriptor<ToscaIntegration> {
        public String getDisplayName() {
            return "";
        }
    }
}
