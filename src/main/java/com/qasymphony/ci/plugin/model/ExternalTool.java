package com.qasymphony.ci.plugin.model;


public interface ExternalTool {

    String getCommand();

    void setCommand(String command);
    String validate() ;

    String getArguments();

    void setArguments(String arguments);


    String getResultPath();
    void setResultPath(String value);

    int execute(Object... params) throws Exception;
}
