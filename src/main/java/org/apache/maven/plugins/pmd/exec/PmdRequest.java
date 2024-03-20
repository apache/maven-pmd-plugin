/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.pmd.exec;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data object to store all configuration options needed to execute PMD
 * as a separate process.
 *
 * <p>This class is intended to be serialized and read back.
 *
 * <p>Some properties might be optional and can be <code>null</code>.
 */
public class PmdRequest implements Serializable {
    private static final long serialVersionUID = -6324416880563476455L;

    private String javaExecutable;

    private String language;
    private String languageVersion;
    private int minimumPriority;
    private String auxClasspath;
    private String suppressMarker;
    private String analysisCacheLocation;
    private List<String> rulesets;
    private String sourceEncoding;
    private List<File> files = new ArrayList<>();

    private boolean showPmdLog;
    private String logLevel;
    private boolean skipPmdError;

    private String excludeFromFailureFile;
    private String targetDirectory;
    private String outputEncoding;
    private String format;
    private String benchmarkOutputLocation;
    private boolean includeXmlInReports;
    private String reportOutputDirectory;

    /**
     * Configure language and language version.
     *
     * @param language the language
     * @param targetJdk the language version, optional, can be <code>null</code>
     */
    public void setLanguageAndVersion(String language, String targetJdk) {
        if ("java".equals(language) || null == language) {
            this.language = "java";
            this.languageVersion = targetJdk;
        } else if ("javascript".equals(language) || "ecmascript".equals(language)) {
            this.language = "ecmascript";
        } else if ("jsp".equals(language)) {
            this.language = "jsp";
        } else {
            this.language = language;
        }
    }

    public void setJavaExecutable(String javaExecutable) {
        this.javaExecutable = javaExecutable;
    }

    public void setMinimumPriority(int minimumPriority) {
        this.minimumPriority = minimumPriority;
    }

    public void setAuxClasspath(String auxClasspath) {
        this.auxClasspath = auxClasspath;
    }

    public void setSuppressMarker(String suppressMarker) {
        this.suppressMarker = suppressMarker;
    }

    public void setAnalysisCacheLocation(String analysisCacheLocation) {
        this.analysisCacheLocation = analysisCacheLocation;
    }

    public void setRulesets(List<String> rulesets) {
        this.rulesets = rulesets;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public void addFiles(Collection<File> files) {
        this.files.addAll(files);
    }

    public void setBenchmarkOutputLocation(String benchmarkOutputLocation) {
        this.benchmarkOutputLocation = benchmarkOutputLocation;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setShowPmdLog(boolean showPmdLog) {
        this.showPmdLog = showPmdLog;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public void setSkipPmdError(boolean skipPmdError) {
        this.skipPmdError = skipPmdError;
    }

    public void setIncludeXmlInReports(boolean includeXmlInReports) {
        this.includeXmlInReports = includeXmlInReports;
    }

    public void setReportOutputDirectory(String reportOutputDirectory) {
        this.reportOutputDirectory = reportOutputDirectory;
    }

    public void setExcludeFromFailureFile(String excludeFromFailureFile) {
        this.excludeFromFailureFile = excludeFromFailureFile;
    }

    public String getJavaExecutable() {
        return javaExecutable;
    }

    public String getLanguage() {
        return language;
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

    public int getMinimumPriority() {
        return minimumPriority;
    }

    public String getAuxClasspath() {
        return auxClasspath;
    }

    public String getSuppressMarker() {
        return suppressMarker;
    }

    public String getAnalysisCacheLocation() {
        return analysisCacheLocation;
    }

    public List<String> getRulesets() {
        return rulesets;
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public List<File> getFiles() {
        return files;
    }

    public String getBenchmarkOutputLocation() {
        return benchmarkOutputLocation;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public String getOutputEncoding() {
        return outputEncoding;
    }

    public String getFormat() {
        return format;
    }

    public boolean isShowPmdLog() {
        return showPmdLog;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public boolean isDebugEnabled() {
        return "debug".equals(logLevel);
    }

    public boolean isSkipPmdError() {
        return skipPmdError;
    }

    public boolean isIncludeXmlInReports() {
        return includeXmlInReports;
    }

    public String getReportOutputDirectory() {
        return reportOutputDirectory;
    }

    public String getExcludeFromFailureFile() {
        return excludeFromFailureFile;
    }
}
