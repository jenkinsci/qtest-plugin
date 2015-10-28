/**
 * 
 */
package com.qasymphony.ci.plugin.parse;

import java.util.List;

import com.qasymphony.ci.plugin.model.AutomationTestResult;

/**
 * @author anpham
 *
 */
public interface TestResultParse {
  public  List<AutomationTestResult> parse() throws Exception;
}
