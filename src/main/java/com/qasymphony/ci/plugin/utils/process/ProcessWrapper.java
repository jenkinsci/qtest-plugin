package com.qasymphony.ci.plugin.utils.process;

import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessWrapper {
    private static final Logger LOG = Logger.getLogger(ProcessWrapper.class.getName());
    private Process process;
    private StreamReader processErrorStream;
    private StreamReader processOutputStream;
    public boolean createProcess(File workingDir, String command, String arguments, String encoding, IStreamConsumer outStreamConsumer, IStreamConsumer errorStreamConsumer) {
        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append(command);
        if (StringUtils.isNotEmpty(arguments)) {
            commandBuilder.append(" ");
            commandBuilder.append(arguments);
        }

        String[] commands;
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            commands = new String[]{"cmd", "/c", commandBuilder.toString()};
        } else {
            commands = new String[]{"/bin/bash", "-c", commandBuilder.toString()};
        }
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        // setting the working directory.
        if (workingDir != null) {
            LOG.log(Level.INFO, String.format("[Command Line] Using working directory %s to start the process.", workingDir.getAbsolutePath()));
            processBuilder.directory(workingDir);
        }
        // start the process.
        try {
            process = processBuilder.start();

            //this.processInputStream = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
            this.processErrorStream = StreamReader.pump(process.getErrorStream(), errorStreamConsumer, encoding);
            this.processOutputStream = StreamReader.pump(process.getInputStream(), outStreamConsumer, encoding);
        } catch (IOException ex) {

        }
        return false;
    }
}
