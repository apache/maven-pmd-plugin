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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import net.sourceforge.pmd.RuleSetReferenceId;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.benchmark.Benchmarker;
import net.sourceforge.pmd.benchmark.TextReport;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.ClasspathClassLoader;
import net.sourceforge.pmd.util.IOUtil;
import net.sourceforge.pmd.util.ResourceLoader;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

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
     * <code>1.8</code>, <code>9</code>, <code>10</code>, <code>11</code>, <code>12</code>, and <code>13</code>.
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

    /** The PMD renderer for collecting violations. */
    private PmdCollectingRenderer renderer;

    /** Helper to exclude violations given as a properties file. */
    private final ExcludeViolationsFromFile excludeFromFile = new ExcludeViolationsFromFile();

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
                executePmdWithClassloader();
                if ( skipEmptyReport )
                {
                    result = renderer.hasViolations();
                    if ( result )
                    {
                        getLog().debug( "Skipping report since skipEmptyReport is true and"
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

    private void executePmdWithClassloader()
        throws MavenReportException
    {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
            executePmd();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    private void executePmd()
        throws MavenReportException
    {
        setupPmdLogging();

        if ( renderer != null )
        {
            // PMD has already been run
            getLog().debug( "PMD has already been run - skipping redundant execution." );
            return;
        }

        try
        {
            excludeFromFile.loadExcludeFromFailuresData( excludeFromFailureFile );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Unable to load exclusions", e );
        }

        // configure ResourceManager
        locator.addSearchPath( FileResourceLoader.ID, project.getFile().getParentFile().getAbsolutePath() );
        locator.addSearchPath( "url", "" );
        locator.setOutputDirectory( rulesetsTargetDirectory );

        renderer = new PmdCollectingRenderer();
        PMDConfiguration pmdConfiguration = getPMDConfiguration();

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
        catch ( ResourceNotFoundException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( FileResourceCreationException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        pmdConfiguration.setRuleSets( StringUtils.join( sets, "," ) );

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
        pmdConfiguration.setSourceEncoding( encoding );

        List<DataSource> dataSources = new ArrayList<>( filesToProcess.size() );
        for ( File f : filesToProcess.keySet() )
        {
            dataSources.add( new FileDataSource( f ) );
        }

        if ( sets.length > 0 )
        {
            processFilesWithPMD( pmdConfiguration, dataSources );
        }
        else
        {
            getLog().debug( "Skipping PMD execution as no rulesets are defined." );
        }

        if ( renderer.hasErrors() )
        {
            if ( !skipPmdError )
            {
                getLog().error( "PMD processing errors:" );
                getLog().error( renderer.getErrorsAsString( getLog().isDebugEnabled() ) );
                throw new MavenReportException( "Found " + renderer.getErrors().size() + " PMD processing errors" );
            }
            getLog().warn( "There are " + renderer.getErrors().size() + " PMD processing errors:" );
            getLog().warn( renderer.getErrorsAsString( getLog().isDebugEnabled() ) );
        }

        removeExcludedViolations( renderer.getViolations() );

        // always write XML report, as this might be needed by the check mojo
        // we need to output it even if the file list is empty or we have no violations
        // so the "check" goals can check for violations
        if ( renderer != null )
        {
            Report report = renderer.asReport();
            writeXmlReport( report );

            // write any other format except for xml and html. xml as been just produced.
            // html format is produced by the maven site formatter. Excluding html here
            // avoids usind PMD's own html formatter, which doesn't fit into the maven site
            // considering the html/css styling
            if ( !isHtml() && !isXml() )
            {
                writeFormattedReport( report );
            }
        }

        if ( benchmark )
        {
            try ( PrintStream benchmarkFileStream = new PrintStream( benchmarkOutputFilename ) )
            {
                ( new TextReport() ).generate( Benchmarker.values(), benchmarkFileStream );
            }
            catch ( FileNotFoundException fnfe )
            {
                getLog().error( "Unable to generate benchmark file: " + benchmarkOutputFilename, fnfe );
            }
        }
    }

    private void removeExcludedViolations( List<RuleViolation> violations )
    {
        getLog().debug( "Removing excluded violations. Using " + excludeFromFile.countExclusions()
            + " configured exclusions." );
        int violationsBefore = violations.size();

        Iterator<RuleViolation> iterator = violations.iterator();
        while ( iterator.hasNext() )
        {
            RuleViolation rv = iterator.next();
            if ( excludeFromFile.isExcludedFromFailure( rv ) )
            {
                iterator.remove();
            }
        }

        int numberOfExcludedViolations = violationsBefore - violations.size();
        getLog().debug( "Excluded " + numberOfExcludedViolations + " violations." );
    }

    private void processFilesWithPMD( PMDConfiguration pmdConfiguration, List<DataSource> dataSources )
            throws MavenReportException
    {
        RuleSetFactory ruleSetFactory = new RuleSetFactory( new ResourceLoader(),
                RulePriority.valueOf( this.minimumPriority ), true, true );
        try
        {
            // load the ruleset once to log out any deprecated rules as warnings
            ruleSetFactory.createRuleSets( pmdConfiguration.getRuleSets() );
        }
        catch ( RuleSetNotFoundException e1 )
        {
            throw new MavenReportException( "The ruleset could not be loaded", e1 );
        }

        try
        {
            getLog().debug( "Executing PMD..." );
            RuleContext ruleContext = new RuleContext();
            PMD.processFiles( pmdConfiguration, ruleSetFactory, dataSources, ruleContext,
                              Arrays.<Renderer>asList( renderer ) );

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "PMD finished. Found " + renderer.getViolations().size() + " violations." );
            }
        }
        catch ( Exception e )
        {
            String message = "Failure executing PMD: " + e.getLocalizedMessage();
            if ( !skipPmdError )
            {
                throw new MavenReportException( message, e );
            }
            getLog().warn( message, e );
        }
        finally
        {
            ClassLoader classLoader = pmdConfiguration.getClassLoader();
            if ( classLoader instanceof ClasspathClassLoader )
            {
                IOUtil.tryCloseClassLoader( classLoader );
            }
        }
    }

    private void generateMavenSiteReport( Locale locale )
        throws MavenReportException
    {
        Sink sink = getSink();
        PmdReportGenerator doxiaRenderer = new PmdReportGenerator( getLog(), sink, getBundle( locale ), aggregate );
        doxiaRenderer.setRenderRuleViolationPriority( renderRuleViolationPriority );
        doxiaRenderer.setRenderViolationsByPriority( renderViolationsByPriority );
        doxiaRenderer.setFiles( filesToProcess );
        doxiaRenderer.setViolations( renderer.getViolations() );
        if ( renderProcessingErrors )
        {
            doxiaRenderer.setProcessingErrors( renderer.getErrors() );
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

    private File writeReport( Report report, Renderer r, String extension ) throws MavenReportException
    {
        if ( r == null )
        {
            return null;
        }

        File targetFile = new File( targetDirectory, "pmd." + extension );
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( targetFile ), getOutputEncoding() ) )
        {
            targetDirectory.mkdirs();

            r.setWriter( writer );
            r.start();
            r.renderFileReport( report );
            r.end();
            r.flush();
        }
        catch ( IOException ioe )
        {
            throw new MavenReportException( ioe.getMessage(), ioe );
        }

        return targetFile;
    }

    /**
     * Use the PMD renderers to render in any format aside from HTML and XML.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeFormattedReport( Report report )
        throws MavenReportException
    {
        Renderer r = createRenderer();
        writeReport( report, r, format );
    }

    /**
     * Use the PMD XML renderer to create the XML report format used by the check mojo later on.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeXmlReport( Report report ) throws MavenReportException
    {
        File targetFile = writeReport( report, new XMLRenderer( getOutputEncoding() ), "xml" );
        if ( includeXmlInSite )
        {
            File siteDir = getReportOutputDirectory();
            siteDir.mkdirs();
            try
            {
                FileUtils.copyFile( targetFile, new File( siteDir, "pmd.xml" ) );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }
    }

    /**
     * Constructs the PMD configuration class, passing it an argument that configures the target JDK.
     *
     * @return the resulting PMD
     * @throws org.apache.maven.reporting.MavenReportException if targetJdk is not supported
     */
    public PMDConfiguration getPMDConfiguration()
        throws MavenReportException
    {
        PMDConfiguration configuration = new PMDConfiguration();
        LanguageVersion languageVersion = null;

        if ( ( "java".equals( language ) || null == language ) && null != targetJdk )
        {
            languageVersion = LanguageRegistry.findLanguageVersionByTerseName( "java " + targetJdk );
            if ( languageVersion == null )
            {
                throw new MavenReportException( "Unsupported targetJdk value '" + targetJdk + "'." );
            }
        }
        else if ( "javascript".equals( language ) || "ecmascript".equals( language ) )
        {
            languageVersion = LanguageRegistry.findLanguageVersionByTerseName( "ecmascript" );
        }
        else if ( "jsp".equals( language ) )
        {
            languageVersion = LanguageRegistry.findLanguageVersionByTerseName( "jsp" );
        }

        if ( languageVersion != null )
        {
            getLog().debug( "Using language " + languageVersion );
            configuration.setDefaultLanguageVersion( languageVersion );
        }

        if ( typeResolution )
        {
            configureTypeResolution( configuration );
        }

        if ( null != suppressMarker )
        {
            configuration.setSuppressMarker( suppressMarker );
        }

        configuration.setBenchmark( benchmark );

        if ( analysisCache )
        {
            configuration.setAnalysisCacheLocation( analysisCacheLocation );
            getLog().debug( "Using analysis cache location: " + analysisCacheLocation );
        }
        else
        {
            configuration.setIgnoreIncrementalAnalysis( true );
        }

        return configuration;
    }

    private void configureTypeResolution( PMDConfiguration configuration ) throws MavenReportException
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
            configuration.prependClasspath( path );
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

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException if no renderer found for the output type
     */
    public final Renderer createRenderer()
        throws MavenReportException
    {
        Renderer result = null;
        if ( "xml".equals( format ) )
        {
            result = new XMLRenderer( getOutputEncoding() );
        }
        else if ( "txt".equals( format ) )
        {
            result = new TextRenderer();
        }
        else if ( "csv".equals( format ) )
        {
            result = new CSVRenderer();
        }
        else if ( "html".equals( format ) )
        {
            result = new HTMLRenderer();
        }
        else if ( !"".equals( format ) && !"none".equals( format ) )
        {
            try
            {
                result = (Renderer) Class.forName( format ).getConstructor( Properties.class ).
                                newInstance( new Properties() );
            }
            catch ( Exception e )
            {
                throw new MavenReportException( "Can't find PMD custom format " + format + ": "
                    + e.getClass().getName(), e );
            }
        }

        return result;
    }
}
