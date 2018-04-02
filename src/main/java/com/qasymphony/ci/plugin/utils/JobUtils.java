package com.qasymphony.ci.plugin.utils;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * @author tamvo
 * @version 28/2/2018 tamvo $
 * @since 1.0
 */
public class JobUtils {

  public static boolean isPipelineJob(Job job) {
    if (job == null) {
      return false;
    }

    if (job instanceof WorkflowJob) {
      return true;
    }

    return false;
  }

  public static boolean isFreeStyleProjectJob(Job job) {
    if (job == null) {
      return false;
    }

    if (job instanceof FreeStyleProject) {
      return true;
    }

    return false;
  }
}