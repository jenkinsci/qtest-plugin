package com.qasymphony.ci.plugin.extension;

import com.qasymphony.ci.plugin.action.PipelineStatisticsAction;
import hudson.Extension;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.Collection;
import java.util.Collections;


@SuppressWarnings("rawtypes")
@Extension
public class JunitQtestPipelineExtension extends TransientActionFactory<WorkflowJob> {
  @Override
  public Class<WorkflowJob> type() {
    return WorkflowJob.class;
  }
  @Override
  public Collection<? extends Action> createFor(WorkflowJob j) {
    return Collections.singleton(new PipelineStatisticsAction(j));
  }
}
