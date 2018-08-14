package com.qasymphony.ci.plugin.utils;

import com.qasymphony.ci.plugin.utils.process.IStreamConsumer;

import java.io.PrintStream;

public class StreamWrapper implements IStreamConsumer {
    private PrintStream stream;
    private boolean errorStream;
    public StreamWrapper(PrintStream stream, boolean errorStream) {
        this.stream = stream;
        this.errorStream = errorStream;
    }
    @Override
    public void consumeLine(String line) {
        if (null != stream) {
            if (this.errorStream) {
                LoggerUtils.formatError(stream, line);
            } else {
                LoggerUtils.printLog(stream, line);
            }
        }
    }
}
