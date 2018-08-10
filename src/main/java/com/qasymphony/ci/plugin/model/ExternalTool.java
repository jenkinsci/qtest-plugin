package com.qasymphony.ci.plugin.model;



public interface ExternalTool {

    String getCommand();

    void setCommand(String command);
    boolean validate() ;

    String getResultPath();
    void setResultPath(String value);
}
