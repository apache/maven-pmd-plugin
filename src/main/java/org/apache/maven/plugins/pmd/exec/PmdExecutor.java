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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.ExcludeViolationsFromFile;
import org.apache.maven.plugins.pmd.PmdCollectingRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.RulesetsFactoryUtils;
import net.sourceforge.pmd.benchmark.TextTimingReportRenderer;
import net.sourceforge.pmd.benchmark.TimeTracker;
import net.sourceforge.pmd.benchmark.TimingReport;
import net.sourceforge.pmd.benchmark.TimingReportRenderer;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.CSVRenderer;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.TextRenderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

/**
 * Executes PMD with the configuration provided via {@link PmdRequest}.
 */
public class PmdExecutor extends Executor
{
    private static final Logger LOG = LoggerFactory.getLogger( PmdExecutor.class );

    public static PmdResult execute( PmdRequest request ) throws MavenReportException
    {
        if ( request.getJavaExecutable() != null )
        {
            return fork( request );
        }

        // make sure the class loaders are correct and call this in the same JVM
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( PmdExecutor.class.getClassLoader() );
            PmdExecutor executor = new PmdExecutor( request );
            return executor.run();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    private static PmdResult fork( PmdRequest request )
            throws MavenReportException
    {
        File basePmdDir = new File ( request.getTargetDirectory(), "pmd" );
        basePmdDir.mkdirs();
        File pmdRequestFile = new File( basePmdDir, "pmdrequest.bin" );
        try ( ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream( pmdRequestFile ) ) )
        {
            out.writeObject( request );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }

        String classpath = buildClasspath();
        ProcessBuilder pb = new ProcessBuilder();
        // note: using env variable instead of -cp cli arg to avoid length limitations under Windows
        pb.environment().put( "CLASSPATH", classpath );
        pb.command().add( request.getJavaExecutable() );
        pb.command().add( PmdExecutor.class.getName() );
        pb.command().add( pmdRequestFile.getAbsolutePath() );

        LOG.debug( "Executing: CLASSPATH={}, command={}", classpath, pb.command() );
        try
        {
            final Process p = pb.start();
            // Note: can't use pb.inheritIO(), since System.out/System.err has been modified after process start
            // and inheritIO would only inherit file handles, not the changed streams.
            ProcessStreamHandler.start( p.getInputStream(), System.out );
            ProcessStreamHandler.start( p.getErrorStream(), System.err );
            int exit = p.waitFor();
            LOG.debug( "PmdExecutor exit code: {}", exit );
            if ( exit != 0 )
            {
                throw new MavenReportException( "PmdExecutor exited with exit code " + exit );
            }
            return new PmdResult( new File( request.getTargetDirectory(), "pmd.xml" ), request.getOutputEncoding() );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new MavenReportException( e.getMessage(), e );
        }
    }

    /**
     * Execute PMD analysis from CLI.
     * 
     * <p>
     * Single arg with the filename to the serialized {@link PmdRequest}.
     * 
     * <p>
     * Exit-code: 0 = success, 1 = failure in executing
     * 
     * @param args
     */
    public static void main( String[] args )
    {
        File requestFile = new File( args[0] );
        try ( ObjectInputStream in = new ObjectInputStream( new FileInputStream( requestFile ) ) )
        {
            PmdRequest request = (PmdRequest) in.readObject();
            PmdExecutor pmdExecutor = new PmdExecutor( request );
            pmdExecutor.setupLogLevel( request.getLogLevel() );
            pmdExecutor.run();
            System.exit( 0 );
        }
        catch ( IOException | ClassNotFoundException | MavenReportException e )
        {
            LOG.error( e.getMessage(), e );
        }
        System.exit( 1 );
    }

    private final PmdRequest request;

    public PmdExecutor( PmdRequest request )
    {
        this.request = Objects.requireNonNull( request );
    }

    private PmdResult run() throws MavenReportException
    {
        setupPmdLogging( request.isShowPmdLog(), request.isColorizedLog(), request.getLogLevel() );

        PMDConfiguration configuration = new PMDConfiguration();
        LanguageVersion languageVersion = null;
        Language language = LanguageRegistry
                .findLanguageByTerseName( request.getLanguage() != null ? request.getLanguage() : "java" );
        if ( language == null )
        {
            throw new MavenReportException( "Unsupported language: " + request.getLanguage() );
        }
        if ( request.getLanguageVersion() != null )
        {
            languageVersion = language.getVersion( request.getLanguageVersion() );
            if ( languageVersion == null )
            {
                throw new MavenReportException( "Unsupported targetJdk value '" + request.getLanguageVersion() + "'." );
            }
        }
        else
        {
            languageVersion = language.getDefaultVersion();
        }
        LOG.debug( "Using language " + languageVersion );
        configuration.setDefaultLanguageVersion( languageVersion );

        try
        {
            configuration.prependClasspath( request.getAuxClasspath() );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        if ( request.getSuppressMarker() != null )
        {
            configuration.setSuppressMarker( request.getSuppressMarker() );
        }
        if ( request.getAnalysisCacheLocation() != null )
        {
            configuration.setAnalysisCacheLocation( request.getAnalysisCacheLocation() );
            LOG.debug( "Using analysis cache location: " + request.getAnalysisCacheLocation() );
        }
        else
        {
            configuration.setIgnoreIncrementalAnalysis( true );
        }

        configuration.setRuleSets( request.getRulesets() );
        if ( request.getBenchmarkOutputLocation() != null )
        {
            configuration.setBenchmark( true );
        }
        List<File> files = request.getFiles();
        List<DataSource> dataSources = new ArrayList<>( files.size() );
        for ( File f : files )
        {
            dataSources.add( new FileDataSource( f ) );
        }

        PmdCollectingRenderer renderer = new PmdCollectingRenderer();

        if ( StringUtils.isBlank( request.getRulesets() ) )
        {
            LOG.debug( "Skipping PMD execution as no rulesets are defined." );
        }
        else
        {
            if ( request.getBenchmarkOutputLocation() != null )
            {
                TimeTracker.startGlobalTracking();
            }

            try
            {
                processFilesWithPMD( configuration, dataSources, renderer );
            }
            finally
            {
                if ( request.getAuxClasspath() != null )
                {
                    ClassLoader classLoader = configuration.getClassLoader();
                    if ( classLoader instanceof Closeable )
                    {
                        Closeable closeable = (Closeable) classLoader;
                        try
                        {
                            closeable.close();
                        }
                        catch ( IOException ex )
                        {
                            // ignore
                        }
                    }
                }
                if ( request.getBenchmarkOutputLocation() != null )
                {
                    TimingReport timingReport = TimeTracker.stopGlobalTracking();
                    writeBenchmarkReport( timingReport, request.getBenchmarkOutputLocation(),
                            request.getOutputEncoding() );
                }
            }
        }

        if ( renderer.hasErrors() )
        {
            if ( !request.isSkipPmdError() )
            {
                LOG.error( "PMD processing errors:" );
                LOG.error( renderer.getErrorsAsString( request.isDebugEnabled() ) );
                throw new MavenReportException( "Found " + renderer.getErrors().size() + " PMD processing errors" );
            }
            LOG.warn( "There are {} PMD processing errors:", renderer.getErrors().size() );
            LOG.warn( renderer.getErrorsAsString( request.isDebugEnabled() ) );
        }

        removeExcludedViolations( renderer.getViolations() );

        Report report = renderer.asReport();
        // always write XML report, as this might be needed by the check mojo
        // we need to output it even if the file list is empty or we have no violations
        // so the "check" goals can check for violations
        writeXmlReport( report );

        // write any other format except for xml and html. xml has just been produced.
        // html format is produced by the maven site formatter. Excluding html here
        // avoids using PMD's own html formatter, which doesn't fit into the maven site
        // considering the html/css styling
        String format = request.getFormat();
        if ( !"html".equals( format ) && !"xml".equals( format ) )
        {
            writeFormattedReport( report );
        }

        return new PmdResult( new File( request.getTargetDirectory(), "pmd.xml" ), request.getOutputEncoding() );
    }

    private void writeBenchmarkReport( TimingReport timingReport, String benchmarkOutputLocation, String encoding )
    {
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( benchmarkOutputLocation ), encoding ) )
        {
            final TimingReportRenderer renderer = new TextTimingReportRenderer();
            renderer.render( timingReport, writer );
        }
        catch ( IOException e )
        {
            LOG.error( "Unable to generate benchmark file: {}", benchmarkOutputLocation, e );
        }
    }

    private void processFilesWithPMD( PMDConfiguration pmdConfiguration, List<DataSource> dataSources,
            PmdCollectingRenderer renderer ) throws MavenReportException
    {
        RuleSetFactory ruleSetFactory = RulesetsFactoryUtils.createFactory(
                RulePriority.valueOf( request.getMinimumPriority() ), true, true );
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
            LOG.debug( "Executing PMD..." );
            RuleContext ruleContext = new RuleContext();
            PMD.processFiles( pmdConfiguration, ruleSetFactory, dataSources, ruleContext,
                    Arrays.<Renderer>asList( renderer ) );

            LOG.debug( "PMD finished. Found {} violations.", renderer.getViolations().size() );
        }
        catch ( Exception e )
        {
            String message = "Failure executing PMD: " + e.getLocalizedMessage();
            if ( !request.isSkipPmdError() )
            {
                throw new MavenReportException( message, e );
            }
            LOG.warn( message, e );
        }
    }

    /**
     * Use the PMD XML renderer to create the XML report format used by the
     * check mojo later on.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeXmlReport( Report report ) throws MavenReportException
    {
        File targetFile = writeReport( report, new XMLRenderer( request.getOutputEncoding() ) );
        if ( request.isIncludeXmlInSite() )
        {
            File siteDir = new File( request.getReportOutputDirectory() );
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

    private File writeReport( Report report, Renderer r ) throws MavenReportException
    {
        if ( r == null )
        {
            return null;
        }

        File targetDir = new File( request.getTargetDirectory() );
        targetDir.mkdirs();
        String extension = r.defaultFileExtension();
        File targetFile = new File( targetDir, "pmd." + extension );
        LOG.debug( "Target PMD output file: {}", targetFile  );
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( targetFile ),
                request.getOutputEncoding() ) )
        {
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
        Renderer renderer = createRenderer( request.getFormat(), request.getOutputEncoding() );
        writeReport( report, renderer );
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException
     *             if no renderer found for the output type
     */
    public static Renderer createRenderer( String format, String outputEncoding ) throws MavenReportException
    {
        LOG.debug( "Renderer requested: {}", format );
        Renderer result = null;
        if ( "xml".equals( format ) )
        {
            result = new XMLRenderer( outputEncoding );
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
                result = (Renderer) Class.forName( format ).getConstructor().newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException(
                        "Can't find PMD custom format " + format + ": " + e.getClass().getName(), e );
            }
        }

        return result;
    }

    private void removeExcludedViolations( List<RuleViolation> violations )
            throws MavenReportException
    {
        ExcludeViolationsFromFile excludeFromFile = new ExcludeViolationsFromFile();

        try
        {
            excludeFromFile.loadExcludeFromFailuresData( request.getExcludeFromFailureFile() );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Unable to load exclusions", e );
        }

        LOG.debug( "Removing excluded violations. Using {} configured exclusions.",
                excludeFromFile.countExclusions() );
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
        LOG.debug( "Excluded {} violations.", numberOfExcludedViolations );
    }
}
