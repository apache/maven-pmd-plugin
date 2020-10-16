package org.apache.maven.plugins.pmd;

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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import net.sourceforge.pmd.PMDVersion;

/**
 * Base class for the PMD reports.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractPmdReport
    extends AbstractMavenReport
{
    // ----------------------------------------------------------------------
    // Configurables
    // ----------------------------------------------------------------------

    /**
     * The output directory for the intermediate XML report.
     */
    @Parameter( property = "project.build.directory", required = true )
    protected File targetDirectory;

    /**
     * The output directory for the final HTML report. Note that this parameter is only evaluated if the goal is run
     * directly from the command line or during the default lifecycle. If the goal is run indirectly as part of a site
     * generation, the output directory configured in the Maven Site Plugin is used instead.
     */
    @Parameter( property = "project.reporting.outputDirectory", required = true )
    protected File outputDirectory;

    /**
     * Set the output format type, in addition to the HTML report. Must be one of: "none", "csv", "xml", "txt" or the
     * full class name of the PMD renderer to use. See the net.sourceforge.pmd.renderers package javadoc for available
     * renderers. XML is produced in any case, since this format is needed
     * for the check goals (pmd:check, pmd:cpd-check).
     */
    @Parameter( property = "format", defaultValue = "xml" )
    protected String format = "xml";

    /**
     * Link the violation line numbers to the source xref. Links will be created automatically if the jxr plugin is
     * being used.
     */
    @Parameter( property = "linkXRef", defaultValue = "true" )
    private boolean linkXRef;

    /**
     * Location of the Xrefs to link to.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}/xref" )
    private File xrefLocation;

    /**
     * Location of the Test Xrefs to link to.
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}/xref-test" )
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
    @Parameter( defaultValue = "${project.compileSourceRoots}" )
    private List<String> compileSourceRoots;

    /**
     * The directories containing the test-sources to be used for PMD.
     * Defaults to <code>project.testCompileSourceRoots</code>
     * @since 3.7
     */
    @Parameter( defaultValue = "${project.testCompileSourceRoots}" )
    private List<String> testSourceRoots;

    /**
     * The project source directories that should be excluded.
     *
     * @since 2.2
     */
    @Parameter
    private File[] excludeRoots;

    /**
     * Run PMD on the tests.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    protected boolean includeTests;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @since 2.2
     */
    @Parameter( property = "aggregate", defaultValue = "false" )
    protected boolean aggregate;

    /**
     * The file encoding to use when reading the Java sources.
     *
     * @since 2.3
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String sourceEncoding;

    /**
     * The file encoding when writing non-HTML reports.
     *
     * @since 2.5
     */
    @Parameter( property = "outputEncoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String outputEncoding;

    /**
     * Whether to include the xml files generated by PMD/CPD in the site.
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "false" )
    protected boolean includeXmlInSite;

    /**
     * Skip the PMD/CPD report generation if there are no violations or duplications found. Defaults to
     * <code>false</code>.
     *
     * <p>Note: the default value was changed from <code>true</code> to <code>false</code> with version 3.13.0.
     *
     * @since 3.1
     */
    @Parameter( defaultValue = "false" )
    protected boolean skipEmptyReport;

    /**
     * File that lists classes and rules to be excluded from failures.
     * For PMD, this is a properties file. For CPD, this
     * is a text file that contains comma-separated lists of classes
     * that are allowed to duplicate.
     *
     * @since 3.7
     */
    @Parameter( property = "pmd.excludeFromFailureFile", defaultValue = "" )
    protected String excludeFromFailureFile;

    /**
     * Redirect PMD log into maven log out.
     * When enabled, the PMD log output is redirected to maven, so that
     * it is visible in the console together with all the other log output.
     * Also, if maven is started with the debug flag (<code>-X</code> or <code>--debug</code>),
     * the PMD logger is also configured for debug.
     *
     * @since 3.9.0
     */
    @Parameter( defaultValue = "true", property = "pmd.showPmdLog" )
    protected boolean showPmdLog = true;

    /**
     * <p>
     * Specify the requirements for this jdk toolchain.
     * This overrules the toolchain selected by the maven-toolchain-plugin.
     * </p>
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
     * The project to analyse.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * The projects in the reactor for aggregation report.
     */
    @Parameter( property = "reactorProjects", readonly = true )
    protected List<MavenProject> reactorProjects;

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls and for dependency resolver API calls.
     */
    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    protected MavenSession session;

    /**
     * Site rendering component for generating the HTML report.
     */
    @Component
    private Renderer siteRenderer;

    @Component
    private ToolchainManager toolchainManager;

    /** The files that are being analyzed. */
    protected Map<File, PmdFileInfo> filesToProcess;

    /**
     * {@inheritDoc}
     */
    @Override
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected String constructXRefLocation( boolean test )
    {
        String location = null;
        if ( linkXRef )
        {
            File xrefLoc = test ? xrefTestLocation : xrefLocation;

            String relativePath =
                PathTool.getRelativePath( outputDirectory.getAbsolutePath(), xrefLoc.getAbsolutePath() );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = ".";
            }
            relativePath = relativePath + "/" + xrefLoc.getName();
            if ( xrefLoc.exists() )
            {
                // XRef was already generated by manual execution of a lifecycle binding
                location = relativePath;
            }
            else
            {
                // Not yet generated - check if the report is on its way
                Reporting reporting = project.getModel().getReporting();
                List<ReportPlugin> reportPlugins = reporting != null
                        ? reporting.getPlugins()
                        : Collections.<ReportPlugin>emptyList();
                for ( ReportPlugin plugin : reportPlugins )
                {
                    String artifactId = plugin.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        location = relativePath;
                    }
                }
            }

            if ( location == null )
            {
                getLog().warn( "Unable to locate Source XRef to link to - DISABLED" );
            }
        }
        return location;
    }

    /**
     * Convenience method to get the list of files where the PMD tool will be executed
     *
     * @return a List of the files where the PMD tool will be executed
     * @throws IOException If an I/O error occurs during construction of the
     *                     canonical pathnames of the files
     */
    protected Map<File, PmdFileInfo> getFilesToProcess()
        throws IOException
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return Collections.emptyMap();
        }

        if ( excludeRoots == null )
        {
            excludeRoots = new File[0];
        }

        Collection<File> excludeRootFiles = new HashSet<>( excludeRoots.length );

        for ( File file : excludeRoots )
        {
            if ( file.isDirectory() )
            {
                excludeRootFiles.add( file );
            }
        }

        List<PmdFileInfo> directories = new ArrayList<>();

        if ( null == compileSourceRoots )
        {
            compileSourceRoots = project.getCompileSourceRoots();
        }
        if ( compileSourceRoots != null )
        {
            for ( String root : compileSourceRoots )
            {
                File sroot = new File( root );
                if ( sroot.exists() )
                {
                    String sourceXref = constructXRefLocation( false );
                    directories.add( new PmdFileInfo( project, sroot, sourceXref ) );
                }
            }
        }

        if ( null == testSourceRoots )
        {
            testSourceRoots = project.getTestCompileSourceRoots();
        }
        if ( includeTests && testSourceRoots != null )
        {
            for ( String root : testSourceRoots )
            {
                File sroot = new File( root );
                if ( sroot.exists() )
                {
                    String testXref = constructXRefLocation( true );
                    directories.add( new PmdFileInfo( project, sroot, testXref ) );
                }
            }
        }
        if ( aggregate )
        {
            for ( MavenProject localProject : reactorProjects )
            {
                List<String> localCompileSourceRoots = localProject.getCompileSourceRoots();
                for ( String root : localCompileSourceRoots )
                {
                    File sroot = new File( root );
                    if ( sroot.exists() )
                    {
                        String sourceXref = constructXRefLocation( false );
                        directories.add( new PmdFileInfo( localProject, sroot, sourceXref ) );
                    }
                }
                if ( includeTests )
                {
                    List<String> localTestCompileSourceRoots = localProject.getTestCompileSourceRoots();
                    for ( String root : localTestCompileSourceRoots )
                    {
                        File sroot = new File( root );
                        if ( sroot.exists() )
                        {
                            String testXref = constructXRefLocation( true );
                            directories.add( new PmdFileInfo( localProject, sroot, testXref ) );
                        }
                    }
                }
            }

        }

        String excluding = getExcludes();
        getLog().debug( "Exclusions: " + excluding );
        String including = getIncludes();
        getLog().debug( "Inclusions: " + including );

        Map<File, PmdFileInfo> files = new TreeMap<>();

        for ( PmdFileInfo finfo : directories )
        {
            getLog().debug( "Searching for files in directory " + finfo.getSourceDirectory().toString() );
            File sourceDirectory = finfo.getSourceDirectory();
            if ( sourceDirectory.isDirectory() && !isDirectoryExcluded( excludeRootFiles, sourceDirectory ) )
            {
                List<File> newfiles = FileUtils.getFiles( sourceDirectory, including, excluding );
                for ( File newfile : newfiles )
                {
                    files.put( newfile.getCanonicalFile(), finfo );
                }
            }
        }

        return files;
    }

    private boolean isDirectoryExcluded( Collection<File> excludeRootFiles, File sourceDirectoryToCheck )
    {
        boolean returnVal = false;
        for ( File excludeDir : excludeRootFiles )
        {
            try
            {
                if ( sourceDirectoryToCheck.getCanonicalPath().startsWith( excludeDir.getCanonicalPath() ) )
                {
                    getLog().debug( "Directory " + sourceDirectoryToCheck.getAbsolutePath()
                                        + " has been excluded as it matches excludeRoot "
                                        + excludeDir.getAbsolutePath() );
                    returnVal = true;
                    break;
                }
            }
            catch ( IOException e )
            {
                getLog().warn( "Error while checking " + sourceDirectoryToCheck
                               + " whether it should be excluded.", e );
            }
        }
        return returnVal;
    }

    /**
     * Gets the comma separated list of effective include patterns.
     *
     * @return The comma separated list of effective include patterns, never <code>null</code>.
     */
    private String getIncludes()
    {
        Collection<String> patterns = new LinkedHashSet<>();
        if ( includes != null )
        {
            patterns.addAll( includes );
        }
        if ( patterns.isEmpty() )
        {
            patterns.add( "**/*.java" );
        }
        return StringUtils.join( patterns.iterator(), "," );
    }

    /**
     * Gets the comma separated list of effective exclude patterns.
     *
     * @return The comma separated list of effective exclude patterns, never <code>null</code>.
     */
    private String getExcludes()
    {
        Collection<String> patterns = new LinkedHashSet<>( FileUtils.getDefaultExcludesAsList() );
        if ( excludes != null )
        {
            patterns.addAll( excludes );
        }
        return StringUtils.join( patterns.iterator(), "," );
    }

    protected boolean isXml()
    {
        return "xml".equals( format );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport()
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return false;
        }

        if ( "pom".equals( project.getPackaging() ) && !aggregate )
        {
            return false;
        }

        // if format is XML, we need to output it even if the file list is empty
        // so the "check" goals can check for failures
        if ( isXml() )
        {
            return true;
        }
        try
        {
            filesToProcess = getFilesToProcess();
            if ( filesToProcess.isEmpty() )
            {
                return false;
            }
        }
        catch ( IOException e )
        {
            getLog().error( e );
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    protected String getSourceEncoding()
    {
        return sourceEncoding;
    }

    /**
     * Gets the effective reporting output files encoding.
     *
     * @return The effective reporting output file encoding, never <code>null</code>.
     * @since 2.5
     */
    @Override
    protected String getOutputEncoding()
    {
        return ( outputEncoding != null ) ? outputEncoding : ReaderFactory.UTF_8;
    }

    protected String determineCurrentRootLogLevel()
    {
        String logLevel = System.getProperty( "org.slf4j.simpleLogger.defaultLogLevel" );
        if ( logLevel == null )
        {
            logLevel = System.getProperty( "maven.logging.root.level" );
        }
        if ( logLevel == null )
        {
            // TODO: logback level
            logLevel = "info";
        }
        return logLevel;
    }

    static String getPmdVersion()
    {
        return PMDVersion.VERSION;
    }

    //TODO remove the part with ToolchainManager lookup once we depend on
    //3.0.9 (have it as prerequisite). Define as regular component field then.
    protected final Toolchain getToolchain()
    {
        Toolchain tc = null;

        if ( jdkToolchain != null )
        {
            // Maven 3.3.1 has plugin execution scoped Toolchain Support
            try
            {
                Method getToolchainsMethod =
                    toolchainManager.getClass().getMethod( "getToolchains", MavenSession.class, String.class,
                                                           Map.class );

                @SuppressWarnings( "unchecked" )
                List<Toolchain> tcs =
                    (List<Toolchain>) getToolchainsMethod.invoke( toolchainManager, session, "jdk",
                                                                  jdkToolchain );

                if ( tcs != null && !tcs.isEmpty() )
                {
                    tc = tcs.get( 0 );
                }
            }
            catch ( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e )
            {
                // ignore
            }
        }

        if ( tc == null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
        }

        return tc;
    }
}