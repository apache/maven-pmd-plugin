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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.pmd.PMDVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Base class for the PMD reports.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractPmdReport extends AbstractMavenReport {
    // ----------------------------------------------------------------------
    // Configurables
    // ----------------------------------------------------------------------

    /**
     * The output directory for the intermediate XML report.
     */
    @Parameter(property = "project.build.directory", required = true)
    protected File targetDirectory;

    /**
     * Set the output format type, in addition to the HTML report. Must be one of: "none", "csv", "xml", "txt" or the
     * full class name of the PMD renderer to use. See the net.sourceforge.pmd.renderers package javadoc for available
     * renderers. XML is produced in any case, since this format is needed
     * for the check goals (pmd:check, pmd:aggregator-check, pmd:cpd-check, pmd:aggregator-cpd-check).
     */
    @Parameter(property = "format", defaultValue = "xml")
    protected String format = "xml";

    /**
     * Link the violation line numbers to the (Test) Source XRef. Links will be created automatically if the JXR plugin is
     * being used.
     */
    @Parameter(property = "linkXRef", defaultValue = "true")
    private boolean linkXRef;

    /**
     * Location where Source XRef is generated for this project.
     * <br>
     * <strong>Default</strong>: {@link #getReportOutputDirectory()} + {@code /xref}
     */
    @Parameter
    private File xrefLocation;

    /**
     * Location where Test Source XRef is generated for this project.
     * <br>
     * <strong>Default</strong>: {@link #getReportOutputDirectory()} + {@code /xref-test}
     */
    @Parameter
    private File xrefTestLocation;

    /**
     * A list of files to exclude from checking. Can contain Ant-style wildcards and double wildcards. Note that these
     * exclusion patterns only operate on the path of a source file relative to its source root directory. In other
     * words, files are excluded based on their package and/or class name. If you want to exclude entire source root
     * directories, use the parameter <code>excludeRoots</code> instead.
     *
     * @since 2.2
     */
    @Parameter
    private List<String> excludes;

    /**
     * A list of files to include from checking. Can contain Ant-style wildcards and double wildcards. Defaults to
     * **\/*.java.
     *
     * @since 2.2
     */
    @Parameter
    private List<String> includes;

    /**
     * Specifies the location of the source directories to be used for PMD.
     * Defaults to <code>project.compileSourceRoots</code>.
     * @since 3.7
     */
    @Parameter(defaultValue = "${project.compileSourceRoots}")
    private List<String> compileSourceRoots;

    /**
     * The directories containing the test-sources to be used for PMD.
     * Defaults to <code>project.testCompileSourceRoots</code>
     * @since 3.7
     */
    @Parameter(defaultValue = "${project.testCompileSourceRoots}")
    private List<String> testSourceRoots;

    /**
     * The project source directories that should be excluded.
     *
     * @since 2.2
     */
    @Parameter
    private File[] excludeRoots;

    /**
     * Run PMD on the tests as well.
     *
     * @since 2.2
     */
    @Parameter(defaultValue = "false")
    protected boolean includeTests;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @since 2.2
     * @deprecated since 3.15.0 Use the goals <code>pmd:aggregate-pmd</code> and <code>pmd:aggregate-cpd</code>
     * instead. See <a href="https://maven.apache.org/plugins/maven-pmd-plugin/faq.html#typeresolution_aggregate">FAQ:
     * Why do I get sometimes false positive and/or false negative violations?</a> for an explanation.
     */
    @Parameter(property = "aggregate", defaultValue = "false")
    @Deprecated
    protected boolean aggregate;

    /**
     * Whether to include the XML files generated by PMD/CPD in the {@link #getReportOutputDirectory()}.
     *
     * @since 3.0
     */
    @Parameter(defaultValue = "false")
    protected boolean includeXmlInReports;

    /**
     * Skip the PMD/CPD report generation if there are no violations or duplications found. Defaults to
     * <code>false</code>.
     *
     * <p>Note: the default value was changed from <code>true</code> to <code>false</code> with version 3.13.0.
     *
     * @since 3.1
     */
    @Parameter(defaultValue = "false")
    protected boolean skipEmptyReport;

    /**
     * File that lists classes and rules to be excluded from failures.
     * For PMD, this is a properties file. For CPD, this
     * is a text file that contains comma-separated lists of classes
     * that are allowed to duplicate.
     *
     * @since 3.7
     */
    @Parameter(property = "pmd.excludeFromFailureFile", defaultValue = "")
    protected String excludeFromFailureFile;

    /**
     * Redirect PMD log into maven log out.
     * When enabled, the PMD log output is redirected to maven, so that
     * it is visible in the console together with all the other log output.
     * Also, if maven is started with the debug flag (<code>-X</code> or <code>--debug</code>),
     * the PMD logger is also configured for debug.
     *
     * @since 3.9.0
     * @deprecated With 3.22.0 and the upgrade to PMD 7, this parameter has no effect anymore. The PMD log
     * is now always redirected into the maven log and this can't be disabled by this parameter anymore.
     * In order to disable the logging, see <a href="https://maven.apache.org/maven-logging.html">Maven Logging</a>.
     * You'd need to start maven with <code>MAVEN_OPTS=-Dorg.slf4j.simpleLogger.log.net.sourceforge.pmd=off mvn &lt;goals&gt;</code>.
     */
    @Parameter(defaultValue = "true", property = "pmd.showPmdLog")
    @Deprecated // (since = "3.22.0", forRemoval = true)
    protected boolean showPmdLog = true;

    /**
     * Used to avoid showing the deprecation warning for "showPmdLog" multiple times.
     */
    private boolean warnedAboutShowPmdLog = false;

    /**
     * <p>
     * Allow for configuration of the jvm used to run PMD via maven toolchains.
     * This permits a configuration where the project is built with one jvm and PMD is executed with another.
     * This overrules the toolchain selected by the maven-toolchain-plugin.
     * </p>
     *
     * <p>Examples:</p>
     * (see <a href="https://maven.apache.org/guides/mini/guide-using-toolchains.html">
     *     Guide to Toolchains</a> for more info)
     *
     * <pre>
     * {@code
     *    <configuration>
     *        ...
     *        <jdkToolchain>
     *            <version>1.11</version>
     *        </jdkToolchain>
     *    </configuration>
     *
     *    <configuration>
     *        ...
     *        <jdkToolchain>
     *            <version>1.8</version>
     *            <vendor>zulu</vendor>
     *        </jdkToolchain>
     *    </configuration>
     *    }
     * </pre>
     *
     * <strong>note:</strong> requires at least Maven 3.3.1
     *
     * @since 3.14.0
     */
    @Parameter
    private Map<String, String> jdkToolchain;

    // ----------------------------------------------------------------------
    // Read-only parameters
    // ----------------------------------------------------------------------

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls and for dependency resolver API calls.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession session;

    /** The files that are being analyzed. */
    protected Map<File, PmdFileInfo> filesToProcess;

    @Override
    protected MavenProject getProject() {
        return project;
    }

    protected List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    protected MojoExecution getMojoExecution() {
        return mojoExecution;
    }

    /**
     * Convenience method to get the list of files where the PMD tool will be executed
     *
     * @return a List of the files where the PMD tool will be executed
     * @throws IOException If an I/O error occurs during construction of the
     *                     canonical pathnames of the files
     */
    protected Map<File, PmdFileInfo> getFilesToProcess() throws IOException {
        if (aggregate && !project.isExecutionRoot()) {
            return Collections.emptyMap();
        }

        if (excludeRoots == null) {
            excludeRoots = new File[0];
        }

        Collection<File> excludeRootFiles = new HashSet<>(excludeRoots.length);

        for (File file : excludeRoots) {
            if (file.isDirectory()) {
                excludeRootFiles.add(file);
            }
        }

        List<PmdFileInfo> directories = new ArrayList<>();

        if (null == compileSourceRoots) {
            compileSourceRoots = project.getCompileSourceRoots();
        }
        if (compileSourceRoots != null) {
            for (String root : compileSourceRoots) {
                File sroot = new File(root);
                if (sroot.exists()) {
                    String sourceXref = linkXRef ? constructXrefLocation(xrefLocation, false) : null;
                    directories.add(new PmdFileInfo(project, sroot, sourceXref));
                }
            }
        }

        if (null == testSourceRoots) {
            testSourceRoots = project.getTestCompileSourceRoots();
        }
        if (includeTests && testSourceRoots != null) {
            for (String root : testSourceRoots) {
                File sroot = new File(root);
                if (sroot.exists()) {
                    String testSourceXref = linkXRef ? constructXrefLocation(xrefTestLocation, true) : null;
                    directories.add(new PmdFileInfo(project, sroot, testSourceXref));
                }
            }
        }
        if (isAggregator()) {
            for (MavenProject localProject : getAggregatedProjects()) {
                List<String> localCompileSourceRoots = localProject.getCompileSourceRoots();
                for (String root : localCompileSourceRoots) {
                    File sroot = new File(root);
                    if (sroot.exists()) {
                        String sourceXref = linkXRef ? constructXrefLocation(xrefLocation, false) : null;
                        directories.add(new PmdFileInfo(localProject, sroot, sourceXref));
                    }
                }
                if (includeTests) {
                    List<String> localTestCompileSourceRoots = localProject.getTestCompileSourceRoots();
                    for (String root : localTestCompileSourceRoots) {
                        File sroot = new File(root);
                        if (sroot.exists()) {
                            String testSourceXref = linkXRef ? constructXrefLocation(xrefTestLocation, true) : null;
                            directories.add(new PmdFileInfo(localProject, sroot, testSourceXref));
                        }
                    }
                }
            }
        }

        String excluding = getExcludes();
        getLog().debug("Exclusions: " + excluding);
        String including = getIncludes();
        getLog().debug("Inclusions: " + including);

        Map<File, PmdFileInfo> files = new TreeMap<>();

        for (PmdFileInfo finfo : directories) {
            getLog().debug("Searching for files in directory "
                    + finfo.getSourceDirectory().toString());
            File sourceDirectory = finfo.getSourceDirectory();
            if (sourceDirectory.isDirectory() && !isDirectoryExcluded(excludeRootFiles, sourceDirectory)) {
                List<File> newfiles = FileUtils.getFiles(sourceDirectory, including, excluding);
                for (File newfile : newfiles) {
                    files.put(newfile.getCanonicalFile(), finfo);
                }
            }
        }

        return files;
    }

    private boolean isDirectoryExcluded(Collection<File> excludedRootFiles, File sourceDirectoryToCheck) {
        for (File excludedDirectory : excludedRootFiles) {
            try {
                if (sourceDirectoryToCheck
                        .getCanonicalFile()
                        .toPath()
                        .startsWith(excludedDirectory.getCanonicalFile().toPath())) {
                    getLog().debug("Directory " + sourceDirectoryToCheck.getAbsolutePath()
                            + " has been excluded as it matches excludeRoot "
                            + excludedDirectory.getAbsolutePath());
                    return true;
                }
            } catch (IOException e) {
                getLog().warn("Error while checking " + sourceDirectoryToCheck + " whether it should be excluded.", e);
            }
        }
        return false;
    }

    /**
     * Gets the comma separated list of effective include patterns.
     *
     * @return The comma separated list of effective include patterns, never <code>null</code>.
     */
    private String getIncludes() {
        Collection<String> patterns = new LinkedHashSet<>();
        if (includes != null) {
            patterns.addAll(includes);
        }
        if (patterns.isEmpty()) {
            patterns.add("**/*.java");
        }
        return StringUtils.join(patterns.iterator(), ",");
    }

    /**
     * Gets the comma separated list of effective exclude patterns.
     *
     * @return The comma separated list of effective exclude patterns, never <code>null</code>.
     */
    private String getExcludes() {
        Collection<String> patterns = new LinkedHashSet<>(FileUtils.getDefaultExcludesAsList());
        if (excludes != null) {
            patterns.addAll(excludes);
        }
        return StringUtils.join(patterns.iterator(), ",");
    }

    protected boolean isXml() {
        return "xml".equals(format);
    }

    protected boolean canGenerateReportInternal() throws MavenReportException {
        if (!showPmdLog && !warnedAboutShowPmdLog) {
            getLog().warn("The parameter \"showPmdLog\" has been deprecated and will be removed."
                    + "Setting it to \"false\" has no effect.");
            warnedAboutShowPmdLog = true;
        }

        if (aggregate && !project.isExecutionRoot()) {
            return false;
        }

        if (!isAggregator() && "pom".equalsIgnoreCase(project.getPackaging())) {
            return false;
        }

        // if format is XML, we need to output it even if the file list is empty
        // so the "check" goals can check for failures
        if (isXml()) {
            return true;
        }
        try {
            filesToProcess = getFilesToProcess();
            if (filesToProcess.isEmpty()) {
                return false;
            }
        } catch (IOException e) {
            throw new MavenReportException("Failed to determine files to process for PMD", e);
        }
        return true;
    }

    static String getPmdVersion() {
        return PMDVersion.VERSION;
    }

    public Map<String, String> getJdkToolchain() {
        return jdkToolchain;
    }

    protected boolean isAggregator() {
        // returning here aggregate for backwards compatibility
        return aggregate;
    }

    // Note: same logic as in m-javadoc-p (MJAVADOC-134)
    protected Collection<MavenProject> getAggregatedProjects() {
        Map<Path, MavenProject> reactorProjectsMap = new HashMap<>();
        for (MavenProject reactorProject : this.reactorProjects) {
            reactorProjectsMap.put(reactorProject.getBasedir().toPath(), reactorProject);
        }

        return modulesForAggregatedProject(project, reactorProjectsMap);
    }

    /**
     * Recursively add the modules of the aggregatedProject to the set of aggregatedModules.
     *
     * @param aggregatedProject the project being aggregated
     * @param reactorProjectsMap map of (still) available reactor projects
     * @throws MavenReportException if any
     */
    private Set<MavenProject> modulesForAggregatedProject(
            MavenProject aggregatedProject, Map<Path, MavenProject> reactorProjectsMap) {
        // Maven does not supply an easy way to get the projects representing
        // the modules of a project. So we will get the paths to the base
        // directories of the modules from the project and compare with the
        // base directories of the projects in the reactor.

        if (aggregatedProject.getModules().isEmpty()) {
            return Collections.singleton(aggregatedProject);
        }

        List<Path> modulePaths = new LinkedList<Path>();
        for (String module : aggregatedProject.getModules()) {
            modulePaths.add(new File(aggregatedProject.getBasedir(), module).toPath());
        }

        Set<MavenProject> aggregatedModules = new LinkedHashSet<>();

        for (Path modulePath : modulePaths) {
            MavenProject module = reactorProjectsMap.remove(modulePath);
            if (module != null) {
                aggregatedModules.addAll(modulesForAggregatedProject(module, reactorProjectsMap));
            }
        }

        return aggregatedModules;
    }
}
