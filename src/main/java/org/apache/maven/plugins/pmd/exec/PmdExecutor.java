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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.ExcludeViolationsFromFile;
import org.apache.maven.plugins.pmd.PmdCollectingRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.benchmark.Benchmarker;
import net.sourceforge.pmd.benchmark.TextReport;
import net.sourceforge.pmd.lang.Language;
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
 * Executes PMD
 */
public class PmdExecutor
{
    private static final Logger LOG = LoggerFactory
            .getLogger( PmdExecutor.class );

    /**
     * This holds a strong reference in case we configured the logger to
     * redirect to slf4j. See {@link #showPmdLog}. Without a strong reference,
     * the logger might be garbage collected and the redirect to slf4j is gone.
     */
    private java.util.logging.Logger julLogger;

    public static PmdResult execute( PmdRequest request )
            throws MavenReportException
    {
        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(
                    PmdExecutor.class.getClassLoader() );
            PmdExecutor executor = new PmdExecutor();
            return executor.run( request );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( origLoader );
        }
    }

    private PmdResult run( PmdRequest request ) throws MavenReportException
    {
        setupPmdLogging( request.isShowPmdLog(), request.isColorizedLog(),
                request.getLogLevel() );

        LOG.info( "Executing PMD now for files:" );
        LOG.info( String.valueOf( request.getFiles() ) );

        PMDConfiguration configuration = new PMDConfiguration();
        LanguageVersion languageVersion = null;
        Language language = LanguageRegistry.findLanguageByTerseName(
                request.getLanguage() != null ? request.getLanguage()
                        : "java" );
        if ( language == null )
        {
            throw new MavenReportException(
                    "Unsupported language: " + request.getLanguage() );
        }
        if ( request.getLanguageVersion() != null )
        {
            languageVersion = language
                    .getVersion( request.getLanguageVersion() );
            if ( languageVersion == null )
            {
                throw new MavenReportException( "Unsupported targetJdk value '"
                        + request.getLanguageVersion() + "'." );
            }
        }
        else
        {
            languageVersion = language.getDefaultVersion();
        }
        LOG.debug( "Using language " + languageVersion );
        configuration.setDefaultLanguageVersion( languageVersion );

        if ( request.getAuxClasspath() != null )
        {
            try
            {
                configuration.prependClasspath( request.getAuxClasspath() );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }
        if ( request.getSuppressMarker() != null )
        {
            configuration.setSuppressMarker( request.getSuppressMarker() );
        }
        if ( request.getAnalysisCacheLocation() != null )
        {
            configuration.setAnalysisCacheLocation(
                    request.getAnalysisCacheLocation() );
            LOG.debug( "Using analysis cache location: "
                    + request.getAnalysisCacheLocation() );
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
            processFilesWithPMD( configuration, dataSources,
                    request.getMinimumPriority(), renderer,
                    request.isSkipPmdError() );
        }

        if ( renderer.hasErrors() )
        {
            if ( !request.isSkipPmdError() )
            {
                LOG.error( "PMD processing errors:" );
                LOG.error( renderer.getErrorsAsString( request.isDebugEnabled() ) );
                throw new MavenReportException(
                        "Found " + renderer.getErrors().size()
                                + " PMD processing errors" );
            }
            LOG.warn( "There are " + renderer.getErrors().size()
                    + " PMD processing errors:" );
            LOG.warn( renderer.getErrorsAsString( request.isDebugEnabled() ) );
        }

        removeExcludedViolations( renderer.getViolations(),
                request.getExcludeFromFailureFile() );

        Report report = renderer.asReport();
        // always write XML report, as this might be needed by the check mojo
        // we need to output it even if the file list is empty or we have no
        // violations
        // so the "check" goals can check for violations
        writeXmlReport( report, request.getTargetDirectory(),
                request.getOutputEncoding(), request.isIncludeXmlInSite(),
                request.getReportOutputDirectory() );

        // write any other format except for xml and html. xml has just been
        // produced.
        // html format is produced by the maven site formatter. Excluding html
        // here
        // avoids using PMD's own html formatter, which doesn't fit into the
        // maven site
        // considering the html/css styling
        String format = request.getFormat();
        if ( !"html".equals( format ) && !"xml".equals( format ) )
        {
            writeFormattedReport( report, format, request.getOutputEncoding(),
                    request.getTargetDirectory() );
        }

        if ( request.getBenchmarkOutputLocation() != null )
        {
            try ( PrintStream benchmarkFileStream = new PrintStream(
                    request.getBenchmarkOutputLocation() ) )
            {
                ( new TextReport() ).generate( Benchmarker.values(), benchmarkFileStream );
            }
            catch ( FileNotFoundException fnfe )
            {
                LOG.error( "Unable to generate benchmark file: "
                        + request.getBenchmarkOutputLocation(), fnfe );
            }
        }

        return new PmdResult(
                new File( request.getTargetDirectory(), "pmd.xml" ),
                request.getOutputEncoding() );
    }

    private void processFilesWithPMD( PMDConfiguration pmdConfiguration,
            List<DataSource> dataSources, int minimumPriority,
            PmdCollectingRenderer renderer, boolean skipPmdError )
            throws MavenReportException
    {
        RuleSetFactory ruleSetFactory = new RuleSetFactory(
                new ResourceLoader(), RulePriority.valueOf( minimumPriority ),
                true, true );
        try
        {
            // load the ruleset once to log out any deprecated rules as warnings
            ruleSetFactory.createRuleSets( pmdConfiguration.getRuleSets() );
        }
        catch ( RuleSetNotFoundException e1 )
        {
            throw new MavenReportException( "The ruleset could not be loaded",
                    e1 );
        }

        try
        {
            LOG.debug( "Executing PMD..." );
            RuleContext ruleContext = new RuleContext();
            PMD.processFiles( pmdConfiguration, ruleSetFactory, dataSources,
                    ruleContext, Arrays.<Renderer>asList( renderer ) );

            LOG.debug( "PMD finished. Found {} violations.", renderer.getViolations().size() );
        }
        catch ( Exception e )
        {
            String message = "Failure executing PMD: "
                    + e.getLocalizedMessage();
            if ( !skipPmdError )
            {
                throw new MavenReportException( message, e );
            }
            LOG.warn( message, e );
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

    /**
     * Use the PMD XML renderer to create the XML report format used by the
     * check mojo later on.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeXmlReport( Report report, String targetDirectory,
            String outputEncoding, boolean includeXmlInSite,
            String reportOutputDirectory ) throws MavenReportException
    {
        File targetFile = writeReport( report,
                new XMLRenderer( outputEncoding ), "xml", targetDirectory,
                outputEncoding );
        if ( includeXmlInSite )
        {
            File siteDir = new File( reportOutputDirectory );
            siteDir.mkdirs();
            try
            {
                FileUtils.copyFile( targetFile,
                        new File( siteDir, "pmd.xml" ) );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }
    }

    private File writeReport( Report report, Renderer r, String extension,
            String targetDirectory, String outputEncoding )
            throws MavenReportException
    {
        if ( r == null )
        {
            return null;
        }

        new File( targetDirectory ).mkdirs();

        File targetFile = new File( targetDirectory, "pmd." + extension );
        try ( Writer writer = new OutputStreamWriter(
                new FileOutputStream( targetFile ), outputEncoding ) )
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

    private void setupPmdLogging( boolean showPmdLog, boolean colorizedLog,
            String logLevel )
    {
        MessageUtils.setColorEnabled( colorizedLog );

        if ( !showPmdLog )
        {
            return;
        }

        java.util.logging.Logger logger = java.util.logging.Logger
                .getLogger( "net.sourceforge.pmd" );

        boolean slf4jBridgeAlreadyAdded = false;
        for ( Handler handler : logger.getHandlers() )
        {
            if ( handler instanceof SLF4JBridgeHandler )
            {
                slf4jBridgeAlreadyAdded = true;
                break;
            }
        }

        if ( slf4jBridgeAlreadyAdded )
        {
            LOG.info( "slf4jBridge is already added" );
            return;
        }

        SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
        SimpleFormatter formatter = new SimpleFormatter();
        handler.setFormatter( formatter );
        logger.setUseParentHandlers( false );
        logger.addHandler( handler );
        handler.setLevel( Level.ALL );
        logger.setLevel( Level.ALL );
        julLogger = logger;
        julLogger.fine(
                "Configured jul-to-slf4j bridge for " + logger.getName() );
    }

    /**
     * Use the PMD renderers to render in any format aside from HTML and XML.
     *
     * @param report
     * @throws MavenReportException
     */
    private void writeFormattedReport( Report report, String format,
            String outputEncoding, String targetDirectory )
            throws MavenReportException
    {
        Renderer renderer = createRenderer( format, outputEncoding );
        writeReport( report, renderer, format, targetDirectory,
                outputEncoding );
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException
     *             if no renderer found for the output type
     */
    public Renderer createRenderer( String format, String outputEncoding )
            throws MavenReportException
    {
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
                result = (Renderer) Class.forName( format ).getConstructor()
                        .newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException( "Can't find PMD custom format "
                        + format + ": " + e.getClass().getName(), e );
            }
        }

        return result;
    }

    private void removeExcludedViolations( List<RuleViolation> violations,
            String excludeFromFailureFile ) throws MavenReportException
    {
        ExcludeViolationsFromFile excludeFromFile = new ExcludeViolationsFromFile();

        try
        {
            excludeFromFile
                    .loadExcludeFromFailuresData( excludeFromFailureFile );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "Unable to load exclusions", e );
        }

        LOG.debug( "Removing excluded violations. Using "
                + excludeFromFile.countExclusions()
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
        LOG.debug( "Excluded " + numberOfExcludedViolations + " violations." );
    }

}
