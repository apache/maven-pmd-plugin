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
import java.util.Properties;

/**
 * Data object to store all configuration options needed to execute CPD
 * as a separate process.
 *
 * <p>This class is intended to be serialized and read back.
 *
 * <p>Some properties might be optional and can be <code>null</code>.
 */
public class CpdRequest implements Serializable {
    private static final long serialVersionUID = -7585852992660240668L;

    private String javaExecutable;

    private int minimumTokens;
    private String language;
    private Properties languageProperties;
    private String sourceEncoding;
    private List<File> files = new ArrayList<>();

    private boolean showPmdLog;
    private String logLevel;

    private String excludeFromFailureFile;
    private String targetDirectory;
    private String outputEncoding;
    private String format;
    private boolean includeXmlInReports;
    private String reportOutputDirectory;

    public void setJavaExecutable(String javaExecutable) {
        this.javaExecutable = javaExecutable;
    }

    public void setMinimumTokens(int minimumTokens) {
        this.minimumTokens = minimumTokens;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLanguageProperties(Properties languageProperties) {
        this.languageProperties = languageProperties;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public void addFiles(Collection<File> files) {
        this.files.addAll(files);
    }

    public void setExcludeFromFailureFile(String excludeFromFailureFile) {
        this.excludeFromFailureFile = excludeFromFailureFile;
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

    public void setIncludeXmlInReports(boolean includeXmlInReports) {
        this.includeXmlInReports = includeXmlInReports;
    }

    public void setReportOutputDirectory(String reportOutputDirectory) {
        this.reportOutputDirectory = reportOutputDirectory;
    }

    public void setShowPmdLog(boolean showPmdLog) {
        this.showPmdLog = showPmdLog;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getJavaExecutable() {
        return javaExecutable;
    }

    public int getMinimumTokens() {
        return minimumTokens;
    }

    public String getLanguage() {
        return language;
    }

    public Properties getLanguageProperties() {
        return languageProperties;
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public List<File> getFiles() {
        return files;
    }

    public String getExcludeFromFailureFile() {
        return excludeFromFailureFile;
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

    public boolean isIncludeXmlInReports() {
        return includeXmlInReports;
    }

    public String getReportOutputDirectory() {
        return reportOutputDirectory;
    }

    public boolean isShowPmdLog() {
        return showPmdLog;
    }

    public String getLogLevel() {
        return logLevel;
    }
}
