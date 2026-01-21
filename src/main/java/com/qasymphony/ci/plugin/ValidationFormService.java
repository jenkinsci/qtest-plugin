package com.qasymphony.ci.plugin;

import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;

public class ValidationFormService {

    public static FormValidation checkUrl(String value, AbstractProject project)
            throws IOException, ServletException {
        if (StringUtils.isEmpty(value))
            return FormValidation.error(ResourceBundle.MSG_INVALID_URL);
        try {
            new URL(value);
            Boolean isQtestUrl = ConfigService.validateQtestUrl(value);
            return isQtestUrl ? FormValidation.ok() : FormValidation.error(ResourceBundle.MSG_INVALID_URL);
        } catch (Exception e) {
            return FormValidation.error(ResourceBundle.MSG_INVALID_URL);
        }
    }

    public static FormValidation checkAppSecretKey(String value, String url, String secretKey, AbstractProject project)
            throws IOException, ServletException {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(url))
            return FormValidation.error(ResourceBundle.MSG_INVALID_API_KEY);
        if (!ConfigService.validateApiKey(url, value, secretKey))
            return FormValidation.error(ResourceBundle.MSG_INVALID_API_KEY);
        return FormValidation.ok();
    }

    public static FormValidation checkSecretKey(String value, AbstractProject project) {
        if (!ConfigService.validateSecretKey(value))
            return FormValidation.error(ResourceBundle.MSG_INVALID_SECRET_KEY);
        return FormValidation.ok();
    }

    public static FormValidation checkProjectName(String value)
            throws IOException, ServletException {
        if (StringUtils.isBlank(value))
            return FormValidation.error(ResourceBundle.MSG_INVALID_PROJECT);
        return FormValidation.ok();
    }

    public static FormValidation checkReleaseName(String value)
            throws IOException, ServletException {
        if (StringUtils.isBlank(value))
            return FormValidation.error(ResourceBundle.MSG_INVALID_RELEASE);
        return FormValidation.ok();
    }

    public static FormValidation checkEnvironment(String value)
            throws IOException, ServletException {
        return FormValidation.ok();
    }

    public static FormValidation checkResultPattern(String value)
            throws IOException, ServletException {
        return FormValidation.ok();
    }

    public static FormValidation checkFakeContainerName(String value) {
        if (!StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        return FormValidation.error(ResourceBundle.MSG_INVALID_CONTAINER);
    }
    public static FormValidation checkExternalCommand(String value) {
        if (!StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        return FormValidation.error(ResourceBundle.MSG_INVALID_EXTERNAL_COMMAND);
    }
    public static FormValidation checkExternalArguments(String value) {
        if (!StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        return FormValidation.error(ResourceBundle.MSG_INVALID_EXTERNAL_ARGUMENTS);
    }
    public static FormValidation checkExternalPathToResults(String value) {
        if (!StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        return FormValidation.error(ResourceBundle.MSG_INVALID_EXTERNAL_RESULT_PATH);
    }
}
