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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.sourceforge.pmd.renderers.Renderer;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.pmd.exec.PmdExecutor;
import org.apache.maven.plugins.pmd.exec.PmdRequest;
import org.apache.maven.plugins.pmd.exec.PmdResult;
import org.apache.maven.plugins.pmd.exec.PmdServiceExecutor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;

/**
 * Creates a PMD site report based on the rulesets and configuration set in the plugin.
 * It can also generate a pmd output file aside from the site report in any of the following formats: xml, csv or txt.
 *
 * @author Brett Porter
 * @version $Id$
 * @since 2.0
 */
@Mojo(name = "pmd", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class PmdReport extends AbstractPmdReport {
    /**
     * The target JDK to analyze based on. Should match the source used in the compiler plugin.
     * Valid values depend on the used PMD version. Most common values are
     * <code>8</code>, <code>11</code>, <code>17</code>, and <code>21</code>.
     *
     * <p>The full list of supported Java versions for each PMD version is available at
     * <a href="https://docs.pmd-code.org/latest/pmd_languages_java.html">Java support (PMD)</a>.</p>
     *
     * <p>You can override the default PMD version by specifying PMD as a dependency,
     * see <a href="examples/upgrading-PMD-at-runtime.html">Upgrading PMD at Runtime</a>.</p>
     *
     * <p>
     *   <b>Note:</b> this parameter is only used if the language parameter is set to <code>java</code>.
     * </p>
     */
    @Parameter(property = "targetJdk", defaultValue = "${maven.compiler.source}")
    private String targetJdk;

    /**
     * The programming language to be analyzed by PMD. Valid values are currently <code>java</code>,
     * <code>javascript</code> and <code>jsp</code>.
     *
     * @since 3.0
     */
    @Parameter(defaultValue = "java")
    private String language;

    /**
     * The rule priority threshold; rules with lower priority than this will not be evaluated.
     *
     * @since 2.1
     */
    @Parameter(property = "minimumPriority", defaultValue = "5")
    private int minimumPriority = 5;

    /**
     * Skip the PMD report generation. Most useful on the command line via "-Dpmd.skip=true".
     *
     * @since 2.1
     */
    @Parameter(property = "pmd.skip", defaultValue = "false")
    private boolean skip;

    /**
     * The PMD rulesets to use. See the
     * <a href="https://pmd.github.io/latest/pmd_rules_java.html">Stock Java Rulesets</a> for a
     * list of available rules.
     * Defaults to a custom ruleset provided by this maven plugin
     * (<code>/rulesets/java/maven-pmd-plugin-default.xml</code>).
     */
    @Parameter
    String[] rulesets = new String[] {"/rulesets/java/maven-pmd-plugin-default.xml"};

    /**
     * Controls whether the project's compile/test classpath should be passed to PMD to enable its type resolution
     * feature.
     *
     * @since 3.0
     */
    @Parameter(property = "pmd.typeResolution", defaultValue = "true")
    private boolean typeResolution;

    /**
     * Controls whether PMD will track benchmark information.
     *
     * @since 3.1
     */
    @Parameter(property = "pmd.benchmark", defaultValue = "false")
    private boolean benchmark;

    /**
     * Benchmark output filename.
     *
     * @since 3.1
     */
    @Parameter(property = "pmd.benchmarkOutputFilename", defaultValue = "${project.build.directory}/pmd-benchmark.txt")
    private String benchmarkOutputFilename;

    /**
     * Source level marker used to indicate whether a RuleViolation should be suppressed. If it is not set, PMD's
     * default will be used, which is <code>NOPMD</code>. See also <a
     * href="https://pmd.github.io/latest/pmd_userdocs_suppressing_warnings.html">PMD &#x2013; Suppressing warnings</a>.
     *
     * @since 3.4
     */
    @Parameter(property = "pmd.suppressMarker")
    private String suppressMarker;

    /**
     * per default pmd executions error are ignored to not break the whole
     *
     * @since 3.1
     */
    @Parameter(property = "pmd.skipPmdError", defaultValue = "true")
    private boolean skipPmdError;

    /**
     * Enables the analysis cache, which speeds up PMD. This
     * requires a cache file that contains the results of the last
     * PMD run. Thus, the cache is only effective if this file is
     * not cleaned between runs.
     *
     * @since 3.8
     */
    @Parameter(property = "pmd.analysisCache", defaultValue = "false")
    private boolean analysisCache;

    /**
     * The location of the analysis cache, if it is enabled.
     * This file contains the results of the last PMD run and must not be cleaned
     * between consecutive PMD runs. Otherwise, the cache is not in use.
     * If the file doesn't exist, PMD executes as if no cache is enabled and
     * all files are analyzed. Otherwise, only changed files will be analyzed again.
     *
     * @since 3.8
     */
    @Parameter(property = "pmd.analysisCacheLocation", defaultValue = "${project.build.directory}/pmd/pmd.cache")
    private String analysisCacheLocation;

    /**
     * Also render processing errors into the HTML report.
     * Processing errors are problems, that PMD encountered while executing the rules.
     * It can be parsing errors or exceptions during rule execution.
     * Processing errors indicate a bug in PMD and the information provided help in
     * reporting and fixing bugs in PMD.
     *
     * @since 3.9.0
     */
    @Parameter(property = "pmd.renderProcessingErrors", defaultValue = "true")
    private boolean renderProcessingErrors = true;

    /**
     * Also render the rule priority into the HTML report.
     *
     * @since 3.10.0
     */
    @Parameter(property = "pmd.renderRuleViolationPriority", defaultValue = "true")
    private boolean renderRuleViolationPriority = true;

    /**
     * Add a section in the HTML report, that groups the found violations by rule priority
     * in addition to grouping by file.
     *
     * @since 3.12.0
     */
    @Parameter(property = "pmd.renderViolationsByPriority", defaultValue = "true")
    private boolean renderViolationsByPriority = true;

    /**
     * Add a section in the HTML report that lists the suppressed violations.
     *
     * @since 3.17.0
     */
    @Parameter(property = "pmd.renderSuppressedViolations", defaultValue = "true")
    private boolean renderSuppressedViolations = true;

    /**
     * Before PMD is executed, the configured rulesets are resolved and copied into this directory.
     * <p>Note: Before 3.13.0, this was by default ${project.build.directory}.
     *
     * @since 3.13.0
     */
    @Parameter(property = "pmd.rulesetsTargetDirectory", defaultValue = "${project.build.directory}/pmd/rulesets")
    private File rulesetsTargetDirectory;

    /**
     * Used to locate configured rulesets. The rulesets could be on the plugin
     * classpath or in the local project file system.
     */
    private final ResourceManager locator;

    /**
     * Internationalization component
     */
    private final I18N i18n;

    private final PmdServiceExecutor serviceExecutor;

    private final ConfigurationService configurationService;

    /**
     * Contains the result of the last PMD execution.
     * It might be <code>null</code> which means, that PMD
     * has not been executed yet.
     */
    private PmdResult pmdResult;

    @Inject
    public PmdReport(
            ResourceManager locator,
            ConfigurationService configurationService,
            I18N i18n,
            PmdServiceExecutor serviceExecutor) {
        this.locator = locator;
        this.configurationService = configurationService;
        this.i18n = i18n;
        this.serviceExecutor = serviceExecutor;
    }

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
        return i18n.getString("pmd-report", locale, "report.pmd." + key);
    }

    /**
     * Configures the PMD rulesets to be used directly.
     * Note: Usually the rulesets are configured via the property.
     *
     * @param rulesets the PMD rulesets to be used.
     * @see #rulesets
     */
    public void setRulesets(String[] rulesets) {
        this.rulesets = Arrays.copyOf(rulesets, rulesets.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeReport(Locale locale) throws MavenReportException {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

            PmdReportRenderer renderer = new PmdReportRenderer(
                    getLog(),
                    getSink(),
                    i18n,
                    locale,
                    filesToProcess,
                    pmdResult.getViolations(),
                    renderRuleViolationPriority,
                    renderViolationsByPriority,
                    isAggregator());
            if (renderSuppressedViolations) {
                renderer.setSuppressedViolations(pmdResult.getSuppressedViolations());
            }
            if (renderProcessingErrors) {
                renderer.setProcessingErrors(pmdResult.getErrors());
            }

            renderer.render();
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
            executePmd();
            if (skipEmptyReport) {
                result = pmdResult.hasViolations();
            }
        }
        return result;
    }

    private void executePmd() throws MavenReportException {
        if (pmdResult != null) {
            // PMD has already been run
            getLog().debug("PMD has already been run - skipping redundant execution.");
            return;
        }

        try {
            filesToProcess = getFilesToProcess();

            if (filesToProcess.isEmpty() && !"java".equals(language)) {
                getLog().warn("No files found to process. Did you forget to add additional source directories?"
                        + " (see also build-helper-maven-plugin)");
            }
        } catch (IOException e) {
            throw new MavenReportException("Can't get file list", e);
        }

        PmdRequest request = new PmdRequest();
        request.setLanguageAndVersion(language, targetJdk);
        request.setRulesets(resolveRulesets());
        request.setAuxClasspath(typeResolution ? determineAuxClasspath() : null);
        request.setSourceEncoding(getInputEncoding());
        request.addFiles(filesToProcess.keySet());
        request.setMinimumPriority(minimumPriority);
        request.setSuppressMarker(suppressMarker);
        request.setBenchmarkOutputLocation(benchmark ? benchmarkOutputFilename : null);
        request.setAnalysisCacheLocation(analysisCache ? analysisCacheLocation : null);
        request.setExcludeFromFailureFile(excludeFromFailureFile);
        request.setTargetDirectory(targetDirectory.getAbsolutePath());
        request.setOutputEncoding(getOutputEncoding());
        request.setFormat(format);
        request.setSkipPmdError(skipPmdError);
        request.setIncludeXmlInReports(includeXmlInReports);
        request.setReportOutputDirectory(getReportOutputDirectory().getAbsolutePath());
        request.setJdkToolchain(getJdkToolchain());

        getLog().info("PMD version: " + AbstractPmdReport.getPmdVersion());
        pmdResult = serviceExecutor.execute(request);
    }

    /**
     * Resolves the configured rulesets and copies them as files into the {@link #rulesetsTargetDirectory}.
     *
     * @return comma separated list of absolute file paths of ruleset files
     * @throws MavenReportException if a ruleset could not be found
     */
    private List<String> resolveRulesets() throws MavenReportException {
        // configure ResourceManager - will search for urls (URLResourceLoader) and files in various directories:
        // in the directory of the current project's pom file - note: extensions might replace the pom file on the fly
        locator.addSearchPath(
                FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath());
        // in the current project's directory
        locator.addSearchPath(FileResourceLoader.ID, project.getBasedir().getAbsolutePath());
        // in the base directory - that's the directory of the initial pom requested to build,
        // e.g. the root of a multi-module build
        locator.addSearchPath(FileResourceLoader.ID, session.getRequest().getBaseDirectory());
        locator.setOutputDirectory(rulesetsTargetDirectory);

        String[] sets = new String[rulesets.length];
        try {
            for (int idx = 0; idx < rulesets.length; idx++) {
                String set = rulesets[idx];
                getLog().debug("Preparing ruleset: " + set);
                String rulesetFilename = determineRulesetFilename(set);
                File ruleset = locator.getResourceAsFile(rulesetFilename, getLocationTemp(set, idx + 1));
                if (null == ruleset) {
                    throw new MavenReportException("Could not resolve " + set);
                }
                sets[idx] = ruleset.getAbsolutePath();
            }
        } catch (ResourceNotFoundException | FileResourceCreationException e) {
            throw new MavenReportException(e.getMessage(), e);
        }
        return Arrays.asList(sets);
    }

    private String determineRulesetFilename(String ruleset) {
        String result = ruleset.trim();
        String lowercase = result.toLowerCase(Locale.ROOT);
        if (lowercase.startsWith("http://") || lowercase.startsWith("https://") || lowercase.endsWith(".xml")) {
            return result;
        }

        // assume last part is a single rule, e.g. myruleset.xml/SingleRule
        if (result.indexOf('/') > -1) {
            String rulesetFilename = result.substring(0, result.lastIndexOf('/'));
            if (rulesetFilename.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                return rulesetFilename;
            }
        }
        // maybe a built-in ruleset name, e.g. java-design -> rulesets/java/design.xml
        int dashIndex = lowercase.indexOf('-');
        if (dashIndex > -1 && lowercase.indexOf('-', dashIndex + 1) == -1) {
            String language = result.substring(0, dashIndex);
            String rulesetName = result.substring(dashIndex + 1);
            return "rulesets/" + language + "/" + rulesetName + ".xml";
        }
        // fallback - no change of the given ruleset specifier
        return result;
    }

    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @param position position in the list of rulesets (1-based)
     * @return a String that contains the absolute file name of the file
     */
    protected String getLocationTemp(String name, int position) {
        String loc = name;
        if (loc.indexOf('/') != -1) {
            loc = loc.substring(loc.lastIndexOf('/') + 1);
        }
        if (loc.indexOf('\\') != -1) {
            loc = loc.substring(loc.lastIndexOf('\\') + 1);
        }

        // MPMD-127 in the case that the rules are defined externally on a URL,
        // we need to replace some special URL characters that cannot be
        // used in filenames on disk or produce awkward filenames.
        // Replace all occurrences of the following characters: ? : & = %
        loc = loc.replaceAll("[\\?\\:\\&\\=\\%]", "_");

        if (loc.endsWith(".xml")) {
            loc = loc.substring(0, loc.length() - 4);
        }
        loc = String.format("%03d-%s.xml", position, loc);

        getLog().debug("Before: " + name + " After: " + loc);
        return loc;
    }

    private String determineAuxClasspath() throws MavenReportException {
        try {
            List<String> classpath = new ArrayList<>();
            if (isAggregator()) {
                List<String> dependencies = new ArrayList<>();
                Collection<MavenProject> aggregatedProjects = getAggregatedProjects();
                for (MavenProject localProject : aggregatedProjects) {
                    configurationService
                            .resolveDependenciesAsFile(localProject, aggregatedProjects, includeTests)
                            .forEach(file -> dependencies.add(file.getAbsolutePath()));
                    // Add the project's classes first
                    classpath.addAll(
                            includeTests
                                    ? localProject.getTestClasspathElements()
                                    : localProject.getCompileClasspathElements());
                }

                // Add the dependencies as last entries
                classpath.addAll(dependencies);

                getLog().debug("Using aggregated aux classpath: " + classpath);
            } else {
                classpath.addAll(
                        includeTests ? project.getTestClasspathElements() : project.getCompileClasspathElements());

                getLog().debug("Using aux classpath: " + classpath);
            }
            return String.join(File.pathSeparator, classpath);
        } catch (Exception e) {
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public String getOutputName() {
        return "pmd";
    }

    @Override
    public String getOutputPath() {
        return "pmd";
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException if no renderer found for the output type
     * @deprecated Use {@link PmdExecutor#createRenderer(String, String)} instead.
     */
    @Deprecated
    public final Renderer createRenderer() throws MavenReportException {
        return PmdExecutor.createRenderer(format, getOutputEncoding());
    }
}
