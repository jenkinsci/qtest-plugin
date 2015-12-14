package com.qasymphony.ci.plugin.parse;

import java.util.List;

import com.qasymphony.ci.plugin.model.AutomationTestResult;

/**
 * @author anpham
 *
 */
public interface TestResultParser {
  List<AutomationTestResult> parse(ParseRequest request) throws Exception;
}
