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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.pmd.exec.PmdExecutor;
import org.apache.maven.plugins.pmd.exec.PmdRequest;
import org.apache.maven.plugins.pmd.exec.PmdResult;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.artifact.filter.resolve.AndFilter;
import org.apache.maven.shared.artifact.filter.resolve.ExclusionsFilter;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import net.sourceforge.pmd.RuleSetReferenceId;

/**
 * Creates a PMD report.
 *
 * @author Brett Porter
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "pmd", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class PmdReport
    extends AbstractPmdReport
{
    /**
     * The target JDK to analyze based on. Should match the source used in the compiler plugin. Valid values
     * with the default PMD version are
     * currently <code>1.3</code>, <code>1.4</code>, <code>1.5</code>, <code>1.6</code>, <code>1.7</code>,
     * <code>1.8</code>, <code>9</code>, <code>10</code>, <code>11</code>, <code>12</code>, <code>13</code>,
     * <code>14</code>, and <code>15</code>.
     *
     * <p> You can override the default PMD version by specifying PMD as a dependency,
     * see <a href="examples/upgrading-PMD-at-runtime.html">Upgrading PMD at Runtime</a>.</p>
     *
     * <p>
     *   <b>Note:</b> this parameter is only used if the language parameter is set to <code>java</code>.
     * </p>
     */
    @Parameter( property = "targetJdk", defaultValue = "${maven.compiler.source}" )
    private String targetJdk;

    /**
     * The programming language to be analyzed by PMD. Valid values are currently <code>java</code>,
     * <code>javascript</code> and <code>jsp</code>.
     *
     * @since 3.0
     */
    @Parameter( defaultValue = "java" )
    private String language;

    /**
     * The rule priority threshold; rules with lower priority than this will not be evaluated.
     *
     * @since 2.1
     */
    @Parameter( property = "minimumPriority", defaultValue = "5" )
    private int minimumPriority = 5;

    /**
     * Skip the PMD report generation. Most useful on the command line via "-Dpmd.skip=true".
     *
     * @since 2.1
     */
    @Parameter( property = "pmd.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * The PMD rulesets to use. See the
     * <a href="https://pmd.github.io/latest/pmd_rules_java.html">Stock Java Rulesets</a> for a
     * list of available rules.
     * Defaults to a custom ruleset provided by this maven plugin
     * (<code>/rulesets/java/maven-pmd-plugin-default.xml</code>).
     */
    @Parameter
    private String[] rulesets = new String[] { "/rulesets/java/maven-pmd-plugin-default.xml" };

    /**
     * Controls whether the project's compile/test classpath should be passed to PMD to enable its type resolution
     * feature.
     *
     * @since 3.0
     */
    @Parameter( property = "pmd.typeResolution", defaultValue = "true" )
    private boolean typeResolution;

    /**
     * Controls whether PMD will track benchmark information.
     *
     * @since 3.1
     */
    @Parameter( property = "pmd.benchmark", defaultValue = "false" )
    private boolean benchmark;

    /**
     * Benchmark output filename.
     *
     * @since 3.1
     */
    @Parameter( property = "pmd.benchmarkOutputFilename",
                    defaultValue = "${project.build.directory}/pmd-benchmark.txt" )
    private String benchmarkOutputFilename;

    /**
     * Source level marker used to indicate whether a RuleViolation should be suppressed. If it is not set, PMD's
     * default will be used, which is <code>NOPMD</code>. See also <a
     * href="https://pmd.github.io/latest/pmd_userdocs_suppressing_warnings.html">PMD &#x2013; Suppressing warnings</a>.
     *
     * @since 3.4
     */
    @Parameter( property = "pmd.suppressMarker" )
    private String suppressMarker;

    /**
     */
    @Component
    private ResourceManager locator;

    /**
     * per default pmd executions error are ignored to not break the whole
     *
     * @since 3.1
     */
    @Parameter( property = "pmd.skipPmdError", defaultValue = "true" )
    private boolean skipPmdError;

    /**
     * Enables the analysis cache, which speeds up PMD. This
     * requires a cache file, that contains the results of the last
     * PMD run. Thus the cache is only effective, if this file is
     * not cleaned between runs.
     *
     * @since 3.8
     */
    @Parameter( property = "pmd.analysisCache", defaultValue = "false" )
    private boolean analysisCache;

    /**
     * The location of the analysis cache, if it is enabled.
     * This file contains the results of the last PMD run and must not be cleaned
     * between consecutive PMD runs. Otherwise the cache is not in use.
     * If the file doesn't exist, PMD executes as if there is no cache enabled and
     * all files are analyzed. Otherwise only changed files will be analyzed again.
     *
     * @since 3.8
     */
    @Parameter( property = "pmd.analysisCacheLocation", defaultValue = "${project.build.directory}/pmd/pmd.cache" )
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
    @Parameter( property = "pmd.renderProcessingErrors", defaultValue = "true" )
    private boolean renderProcessingErrors = true;

    /**
     * Also render the rule priority into the HTML report.
     *
     * @since 3.10.0
     */
    @Parameter( property = "pmd.renderRuleViolationPriority", defaultValue = "true" )
    private boolean renderRuleViolationPriority = true;

    /**
     * Add a section in the HTML report, that groups the found violations by rule priority
     * in addition to grouping by file.
     *
     * @since 3.12.0
     */
    @Parameter( property = "pmd.renderViolationsByPriority", defaultValue = "true" )
    private boolean renderViolationsByPriority = true;

    /**
     * Before PMD is executed, the configured rulesets are resolved and copied into this directory.
     * <p>Note: Before 3.13.0, this was by default ${project.build.directory}.
     *
     * @since 3.13.0
     */
    @Parameter( property = "pmd.rulesetsTargetDirectory", defaultValue = "${project.build.directory}/pmd/rulesets" )
    private File rulesetsTargetDirectory;

    @Component
    private DependencyResolver dependencyResolver;

    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;

    private PmdRequest request;
    private PmdResult pmdResult;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.name" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.pmd.description" );
    }

    /**
     * Configures the PMD rulesets to be used directly.
     * Note: Usually the rulesets are configured via the property.
     *
     * @param rulesets the PMD rulesets to be used.
     * @see #rulesets
     */
    public void setRulesets( String[] rulesets )
    {
        this.rulesets = Arrays.copyOf( rulesets, rulesets.length );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        try
        {
            execute( locale );
        }
        finally
        {
            if ( getSink() != null )
            {
                getSink().close();
            }
        }
    }

    private void execute( Locale locale )
        throws MavenReportException
    {
        if ( !skip && canGenerateReport() )
        {
            ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

                generateMavenSiteReport( locale );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( origLoader );
            }
        }
    }

    @Override
    public boolean canGenerateReport()
    {
        if ( skip )
        {
            return false;
        }

        boolean result = super.canGenerateReport();
        if ( result )
        {
            try
            {
                executePmd();
                if ( skipEmptyReport )
                {
                    result = pmdResult.hasViolations();
                    if ( result )
                    {
                        getLog().debug( "Skipping report since skipEmptyReport is true and "
                                            + "there are no PMD violations." );
                    }
                }
            }
            catch ( MavenReportException e )
            {
                throw new RuntimeException( e );
            }
        }
        return result;
    }

    private void executePmd()
        throws MavenReportException
    {
        if ( pmdResult != null )
        {
            // PMD has already been run
            getLog().debug( "PMD has already been run - skipping redundant execution." );
            return;
        }

        request = new PmdRequest();
        updatePmdRequest();

        request.setExcludeFromFailureFile( excludeFromFailureFile );

        // configure ResourceManager
        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        locator.addSearchPath( "url", "" );
        locator.setOutputDirectory( rulesetsTargetDirectory );

        String[] sets = new String[rulesets.length];
        try
        {
            for ( int idx = 0; idx < rulesets.length; idx++ )
            {
                String set = rulesets[idx];
                getLog().debug( "Preparing ruleset: " + set );
                RuleSetReferenceId id = new RuleSetReferenceId( set );
                File ruleset = locator.getResourceAsFile( id.getRuleSetFileName(), getLocationTemp( set ) );
                if ( null == ruleset )
                {
                    throw new MavenReportException( "Could not resolve " + set );
                }
                sets[idx] = ruleset.getAbsolutePath();
            }
        }
        catch ( ResourceNotFoundException | FileResourceCreationException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        String resolvedRulesets = StringUtils.join( sets, "," );
        request.setRulesets( resolvedRulesets );

        try
        {
            if ( filesToProcess == null )
            {
                filesToProcess = getFilesToProcess();
            }

            if ( filesToProcess.isEmpty() && !"java".equals( language ) )
            {
                getLog().warn( "No files found to process. Did you add your additional source folders like javascript?"
                                   + " (see also build-helper-maven-plugin)" );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't get file list", e );
        }

        String encoding = getSourceEncoding();
        if ( StringUtils.isEmpty( encoding ) )
        {
            encoding = ReaderFactory.FILE_ENCODING;
            if ( !filesToProcess.isEmpty() )
            {
                getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
            }
        }
        request.setSourceEncoding( encoding );

        for ( File f : filesToProcess.keySet() )
        {
            request.addFile( f );
        }
        
        request.setMinimumPriority( this.minimumPriority );

        processFilesWithPMD();
    }

    private void processFilesWithPMD() throws MavenReportException
    {
        request.setTargetDirectory( targetDirectory.getAbsolutePath() );
        request.setOutputEncoding( getOutputEncoding() );
        request.setFormat( format );
        request.setShowPmdLog( showPmdLog );
        request.setColorizedLog( MessageUtils.isColorEnabled() );
        request.setSkipPmdError( skipPmdError );
        request.setIncludeXmlInSite( includeXmlInSite );
        request.setReportOutputDirectory( getReportOutputDirectory().getAbsolutePath() );
        
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
        request.setLogLevel( logLevel );

        pmdResult = PmdExecutor.execute( request );
    }

    private void generateMavenSiteReport( Locale locale )
        throws MavenReportException
    {
        Sink sink = getSink();
        PmdReportGenerator doxiaRenderer = new PmdReportGenerator( getLog(), sink, getBundle( locale ), aggregate );
        doxiaRenderer.setRenderRuleViolationPriority( renderRuleViolationPriority );
        doxiaRenderer.setRenderViolationsByPriority( renderViolationsByPriority );
        doxiaRenderer.setFiles( filesToProcess );
        doxiaRenderer.setViolations( pmdResult.getViolations() );
        if ( renderProcessingErrors )
        {
            doxiaRenderer.setProcessingErrors( pmdResult.getErrors() );
        }

        try
        {
            doxiaRenderer.beginDocument();
            doxiaRenderer.render();
            doxiaRenderer.endDocument();
        }
        catch ( IOException e )
        {
            getLog().warn( "Failure creating the report: " + e.getLocalizedMessage(), e );
        }
    }

    /**
     * Convenience method to get the location of the specified file name.
     *
     * @param name the name of the file whose location is to be resolved
     * @return a String that contains the absolute file name of the file
     */
    protected String getLocationTemp( String name )
    {
        String loc = name;
        if ( loc.indexOf( '/' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '/' ) + 1 );
        }
        if ( loc.indexOf( '\\' ) != -1 )
        {
            loc = loc.substring( loc.lastIndexOf( '\\' ) + 1 );
        }

        // MPMD-127 in the case that the rules are defined externally on a url
        // we need to replace some special url characters that cannot be
        // used in filenames on disk or produce ackward filenames.
        // replace all occurrences of the following characters: ? : & = %
        loc = loc.replaceAll( "[\\?\\:\\&\\=\\%]", "_" );

        if ( !loc.endsWith( ".xml" ) )
        {
            loc = loc + ".xml";
        }

        getLog().debug( "Before: " + name + " After: " + loc );
        return loc;
    }

    /**
     * Constructs the PMD configuration class, passing it an argument that configures the target JDK.
     *
     * @return the resulting PMD
     * @throws MavenReportException when a problem during dependency resolution occurs
     */
    private void updatePmdRequest() throws MavenReportException
    {
        if ( ( "java".equals( language ) || null == language ) && null != targetJdk )
        {
            request.setLanguage( "java" );
            request.setLanguageVersion( targetJdk );
        }
        else if ( "javascript".equals( language ) || "ecmascript".equals( language ) )
        {
            request.setLanguage( "ecmascript" );
        }
        else if ( "jsp".equals( language ) )
        {
            request.setLanguage( "jsp" );
        }

        if ( typeResolution )
        {
            configureTypeResolution();
        }

        if ( null != suppressMarker )
        {
            request.setSuppressMarker( suppressMarker );
        }

        if ( benchmark )
        {
            request.setBenchmarkOutputLocation( benchmarkOutputFilename );
        }

        if ( analysisCache )
        {
            request.setAnalysisCacheLocation( analysisCacheLocation );
        }
    }

    private void configureTypeResolution() throws MavenReportException
    {
        try
        {
            List<String> classpath = new ArrayList<>();
            if ( aggregate )
            {
                List<String> dependencies = new ArrayList<>();

                // collect exclusions for projects within the reactor
                // if module a depends on module b and both are in the reactor
                // then we don't want to resolve the dependency as an artifact.
                List<String> exclusionPatterns = new ArrayList<>();
                for ( MavenProject localProject : reactorProjects )
                {
                    exclusionPatterns.add( localProject.getGroupId() + ":" + localProject.getArtifactId() );
                }
                TransformableFilter filter = new AndFilter( Arrays.asList(
                        new ExclusionsFilter( exclusionPatterns ),
                        includeTests ? ScopeFilter.including( "test" ) : ScopeFilter.including( "compile" )
                ) );

                for ( MavenProject localProject : reactorProjects )
                {
                    ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
                            session.getProjectBuildingRequest() );

                    Iterable<ArtifactResult> resolvedDependencies = dependencyResolver.resolveDependencies(
                            buildingRequest, localProject.getModel(), filter );

                    for ( ArtifactResult resolvedArtifact : resolvedDependencies )
                    {
                        dependencies.add( resolvedArtifact.getArtifact().getFile().toString() );
                    }

                    List<String> projectCompileClasspath = includeTests ? localProject.getTestClasspathElements()
                            : localProject.getCompileClasspathElements();
                    // Add the project's target folder first
                    classpath.addAll( projectCompileClasspath );
                    if ( !localProject.isExecutionRoot() )
                    {
                        for ( String path : projectCompileClasspath )
                        {
                            File pathFile = new File( path );
                            String[] children = pathFile.list();

                            if ( !pathFile.exists() || ( children != null && children.length == 0 ) )
                            {
                                getLog().warn( "The project " + localProject.getArtifactId()
                                    + " does not seem to be compiled. PMD results might be inaccurate." );
                            }
                        }
                    }

                }

                // Add the dependencies as last entries
                classpath.addAll( dependencies );

                getLog().debug( "Using aggregated aux classpath: " + classpath );
            }
            else
            {
                classpath.addAll( includeTests ? project.getTestClasspathElements()
                        : project.getCompileClasspathElements() );
                getLog().debug( "Using aux classpath: " + classpath );
            }
            String path = StringUtils.join( classpath.iterator(), File.pathSeparator );
            request.setAuxClasspath( path );
        }
        catch ( Exception e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName()
    {
        return "pmd";
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "pmd-report", locale, PmdReport.class.getClassLoader() );
    }
}
