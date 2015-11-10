package com.qasymphony.ci.plugin.extension;

import com.qasymphony.ci.plugin.action.StatisticsAction;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Register a menu in left sidebar of jenkins
 *
 * @author trongle
 * @version 10/21/2015 1:45 PM trongle $
 * @since 1.0
 */
@Extension
public class JunitQtestExtension extends TransientProjectActionFactory {
  @Override
  public Collection<? extends Action> createFor(@SuppressWarnings("rawtypes") AbstractProject target) {
    final List<StatisticsAction> projectActions = target.getActions(StatisticsAction.class);
    final ArrayList<Action> actions = new ArrayList<>();
    if (projectActions.isEmpty()) {
      final StatisticsAction newAction = new StatisticsAction(target);
      actions.add(newAction);
      return actions;
    } else {
      return projectActions;
    }
  }
}
