package com.qasymphony.ci.plugin.model;

public class Glob {
    private String baseDir;
    private String pattern;

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Glob () {}
    public Glob (String baseDir, String pattern) {
        this.baseDir = baseDir;
        this.pattern = pattern;
    }
}
