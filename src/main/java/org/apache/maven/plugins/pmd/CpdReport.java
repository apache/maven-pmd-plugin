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
package org.apache.maven.plugins.pmd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Properties;

import net.sourceforge.pmd.cpd.JavaTokenizer;
import net.sourceforge.pmd.cpd.renderer.CPDRenderer;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.pmd.exec.CpdExecutor;
import org.apache.maven.plugins.pmd.exec.CpdRequest;
import org.apache.maven.plugins.pmd.exec.CpdResult;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.i18n.I18N;

/**
 * Creates a report for PMD's Copy/Paste Detector (CPD) tool.
 * It can also generate a cpd results file in any of these formats: xml, csv or txt.
 *
 * <p>See <a href="https://pmd.github.io/latest/pmd_userdocs_cpd.html">Finding duplicated code</a>
 * for more details.
 *
 * @author Mike Perham
 * @version $Id$
 * @since 2.0
 */
@Mojo(name = "cpd", threadSafe = true)
public class CpdReport extends AbstractPmdReport {
    /**
     * The programming language to be analyzed by CPD. Valid values are currently <code>java</code>,
     * <code>javascript</code> or <code>jsp</code>.
     *
     * @since 3.5
     */
    @Parameter(defaultValue = "java")
    private String language;

    /**
     * The minimum number of tokens that need to be duplicated before it causes a violation.
     */
    @Parameter(property = "minimumTokens", defaultValue = "100")
    private int minimumTokens;

    /**
     * Skip the CPD report generation. Most useful on the command line via "-Dcpd.skip=true".
     *
     * @since 2.1
     */
    @Parameter(property = "cpd.skip", defaultValue = "false")
    private boolean skip;

    /**
     * If true, CPD ignores literal value differences when evaluating a duplicate block. This means that
     * <code>foo=42;</code> and <code>foo=43;</code> will be seen as equivalent. You may want to run PMD with this
     * option off to start with and then switch it on to see what it turns up.
     *
     * @since 2.5
     */
    @Parameter(property = "cpd.ignoreLiterals", defaultValue = "false")
    private boolean ignoreLiterals;

    /**
     * Similar to <code>ignoreLiterals</code> but for identifiers; i.e., variable names, methods names, and so forth.
     *
     * @since 2.5
     */
    @Parameter(property = "cpd.ignoreIdentifiers", defaultValue = "false")
    private boolean ignoreIdentifiers;

    /**
     * If true, CPD ignores annotations.
     *
     * @since 3.11.0
     */
    @Parameter(property = "cpd.ignoreAnnotations", defaultValue = "false")
    private boolean ignoreAnnotations;

    /**
     * Internationalization component
     */
    @Component
    private I18N i18n;

    /**
     * Contains the result of the last CPD execution.
     * It might be <code>null</code> which means, that CPD
     * has not been executed yet.
     */
    private CpdResult cpdResult;

    /** {@inheritDoc} */
    public String getName(Locale locale) {
        return getI18nString(locale, "name");
    }

    /** {@inheritDoc} */
    public String getDescription(Locale locale) {
        return getI18nString(locale, "description");
    }

    /**
     * @param locale The locale
     * @param key The key to search for
     * @return The text appropriate for the locale.
     */
    protected String getI18nString(Locale locale, String key) {
        return i18n.getString("cpd-report", locale, "report.cpd." + key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeReport(Locale locale) throws MavenReportException {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            CpdReportRenderer r = new CpdReportRenderer(
                    getSink(), i18n, locale, filesToProcess, cpdResult.getDuplications(), isAggregator());
            r.render();
        } finally {
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }

    @Override
    public boolean canGenerateReport() throws MavenReportException {
        if (skip) {
            return false;
        }

        boolean result = canGenerateReportInternal();
        if (result) {
            executeCpd();
            if (skipEmptyReport) {
                result = cpdResult.hasDuplications();
            }
        }
        return result;
    }

    private void executeCpd() throws MavenReportException {
        if (cpdResult != null) {
            // CPD has already been run
            getLog().debug("CPD has already been run - skipping redundant execution.");
            return;
        }

        Properties languageProperties = new Properties();
        if (ignoreLiterals) {
            languageProperties.setProperty(JavaTokenizer.IGNORE_LITERALS, "true");
        }
        if (ignoreIdentifiers) {
            languageProperties.setProperty(JavaTokenizer.IGNORE_IDENTIFIERS, "true");
        }
        if (ignoreAnnotations) {
            languageProperties.setProperty(JavaTokenizer.IGNORE_ANNOTATIONS, "true");
        }
        try {
            filesToProcess = getFilesToProcess();

            CpdRequest request = new CpdRequest();
            request.setMinimumTokens(minimumTokens);
            request.setLanguage(language);
            request.setLanguageProperties(languageProperties);
            request.setSourceEncoding(getInputEncoding());
            request.addFiles(filesToProcess.keySet());

            request.setShowPmdLog(showPmdLog);
            request.setLogLevel(determineCurrentRootLogLevel());

            request.setExcludeFromFailureFile(excludeFromFailureFile);
            request.setTargetDirectory(targetDirectory.getAbsolutePath());
            request.setOutputEncoding(getOutputEncoding());
            request.setFormat(format);
            request.setIncludeXmlInReports(includeXmlInReports);
            request.setReportOutputDirectory(getReportOutputDirectory().getAbsolutePath());

            Toolchain tc = getToolchain();
            if (tc != null) {
                getLog().info("Toolchain in maven-pmd-plugin: " + tc);
                String javaExecutable = tc.findTool("java"); // NOI18N
                request.setJavaExecutable(javaExecutable);
            }

            getLog().info("PMD version: " + AbstractPmdReport.getPmdVersion());
            cpdResult = CpdExecutor.execute(request);
        } catch (UnsupportedEncodingException e) {
            throw new MavenReportException("Encoding '" + getInputEncoding() + "' is not supported.", e);
        } catch (IOException e) {
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName() {
        return "cpd";
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException if no renderer found for the output type
     * @deprecated Use {@link CpdExecutor#createRenderer(String, String)} instead.
     */
    @Deprecated
    public CPDRenderer createRenderer() throws MavenReportException {
        return CpdExecutor.createRenderer(format, getOutputEncoding());
    }
}
