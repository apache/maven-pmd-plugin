package org.apache.maven.plugins.pmd.exec;

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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data object to store all information needed to execute PMD
 * as a separate process.
 */
public class PmdRequest implements Serializable
{
    private static final long serialVersionUID = -6324416880563476455L;

    private String language;
    private String languageVersion;
    private int minimumPriority;
    private String auxClasspath;
    private String suppressMarker;
    private String analysisCacheLocation;
    private String rulesets;
    private String sourceEncoding;
    private List<File> files = new ArrayList<>();
    private String benchmarkOutputLocation;

    private String targetDirectory;
    private String outputEncoding;
    private String format;
    private boolean showPmdLog;
    private boolean colorizedLog;
    private String logLevel;
    private boolean skipPmdError;
    private boolean includeXmlInSite;
    private String reportOutputDirectory;
    private String excludeFromFailureFile;

    public void setLanguage( String language )
    {
        this.language = language;
    }

    public void setLanguageVersion( String languageVersion )
    {
        this.languageVersion = languageVersion;
    }

    public void setMinimumPriority( int minimumPriority )
    {
        this.minimumPriority = minimumPriority;
    }

    public void setAuxClasspath( String auxClasspath )
    {
        this.auxClasspath = auxClasspath;
    }

    public void setSuppressMarker( String suppressMarker )
    {
        this.suppressMarker = suppressMarker;
    }

    public void setAnalysisCacheLocation( String analysisCacheLocation )
    {
        this.analysisCacheLocation = analysisCacheLocation;
    }

    public void setRulesets( String rulesets )
    {
        this.rulesets = rulesets;
    }

    public void setSourceEncoding( String sourceEncoding )
    {
        this.sourceEncoding = sourceEncoding;
    }

    public void addFile( File file )
    {
        this.files.add( file );
    }

    public void setBenchmarkOutputLocation( String benchmarkOutputLocation )
    {
        this.benchmarkOutputLocation = benchmarkOutputLocation;
    }

    public void setTargetDirectory( String targetDirectory )
    {
        this.targetDirectory = targetDirectory;
    }

    public void setOutputEncoding( String outputEncoding )
    {
        this.outputEncoding = outputEncoding;
    }

    public void setFormat( String format )
    {
        this.format = format;
    }

    public void setShowPmdLog( boolean showPmdLog )
    {
        this.showPmdLog = showPmdLog;
    }

    public void setColorizedLog( boolean colorizedLog )
    {
        this.colorizedLog = colorizedLog;
    }

    public void setLogLevel( String logLevel )
    {
        this.logLevel = logLevel;
    }

    public void setSkipPmdError( boolean skipPmdError )
    {
        this.skipPmdError = skipPmdError;
    }

    public void setIncludeXmlInSite( boolean includeXmlInSite )
    {
        this.includeXmlInSite = includeXmlInSite;
    }

    public void setReportOutputDirectory( String reportOutputDirectory )
    {
        this.reportOutputDirectory = reportOutputDirectory;
    }

    public void setExcludeFromFailureFile( String excludeFromFailureFile )
    {
        this.excludeFromFailureFile = excludeFromFailureFile;
    }





    public String getLanguage()
    {
        return language;
    }

    public String getLanguageVersion()
    {
        return languageVersion;
    }

    public int getMinimumPriority()
    {
        return minimumPriority;
    }

    public String getAuxClasspath()
    {
        return auxClasspath;
    }

    public String getSuppressMarker()
    {
        return suppressMarker;
    }

    public String getAnalysisCacheLocation()
    {
        return analysisCacheLocation;
    }

    public String getRulesets()
    {
        return rulesets;
    }

    public String getSourceEncoding()
    {
        return sourceEncoding;
    }

    public List<File> getFiles()
    {
        return files;
    }

    public String getBenchmarkOutputLocation()
    {
        return benchmarkOutputLocation;
    }

    public String getTargetDirectory()
    {
        return targetDirectory;
    }

    public String getOutputEncoding()
    {
        return outputEncoding;
    }

    public String getFormat()
    {
        return format;
    }

    public boolean isShowPmdLog()
    {
        return showPmdLog;
    }

    public boolean isColorizedLog()
    {
        return colorizedLog;
    }

    public String getLogLevel()
    {
        return logLevel;
    }

    public boolean isDebugEnabled()
    {
        return "debug".equals( logLevel );
    }

    public boolean isSkipPmdError()
    {
        return skipPmdError;
    }

    public boolean isIncludeXmlInSite()
    {
        return includeXmlInSite;
    }

    public String getReportOutputDirectory()
    {
        return reportOutputDirectory;
    }

    public String getExcludeFromFailureFile()
    {
        return excludeFromFailureFile;
    }
}
