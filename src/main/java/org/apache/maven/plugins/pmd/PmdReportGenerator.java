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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import net.sourceforge.pmd.Report.ProcessingError;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleViolation;

/**
 * Render the PMD violations into Doxia events.
 *
 * @author Brett Porter
 * @version $Id$
 */
public class PmdReportGenerator
{
    private Log log;

    private Sink sink;

    private String currentFilename;

    private ResourceBundle bundle;

    private Set<RuleViolation> violations = new HashSet<>();

    private List<ProcessingError> processingErrors = new ArrayList<>();

    private boolean aggregate;

    private boolean renderRuleViolationPriority;

    private boolean renderViolationsByPriority;

    private Map<File, PmdFileInfo> files;

    // private List<Metric> metrics = new ArrayList<Metric>();

    public PmdReportGenerator( Log log, Sink sink, ResourceBundle bundle, boolean aggregate )
    {
        this.log = log;
        this.sink = sink;
        this.bundle = bundle;
        this.aggregate = aggregate;
    }

    private String getTitle()
    {
        return bundle.getString( "report.pmd.title" );
    }

    public void setViolations( Collection<RuleViolation> violations )
    {
        this.violations = new HashSet<>( violations );
    }

    public List<RuleViolation> getViolations()
    {
        return new ArrayList<>( violations );
    }

    public void setProcessingErrors( Collection<ProcessingError> errors )
    {
        this.processingErrors = new ArrayList<>( errors );
    }

    public List<ProcessingError> getProcessingErrors()
    {
        return processingErrors;
    }

    // public List<Metric> getMetrics()
    // {
    // return metrics;
    // }
    //
    // public void setMetrics( List<Metric> metrics )
    // {
    // this.metrics = metrics;
    // }

    private String shortenFilename( String filename, PmdFileInfo fileInfo )
    {
        String result = filename;
        if ( fileInfo != null && fileInfo.getSourceDirectory() != null )
        {
            result = StringUtils.substring( result, fileInfo.getSourceDirectory().getAbsolutePath().length() + 1 );
        }
        return StringUtils.replace( result, "\\", "/" );
    }

    private String makeFileSectionName( String filename, PmdFileInfo fileInfo )
    {
        if ( aggregate && fileInfo != null && fileInfo.getProject() != null )
        {
            return fileInfo.getProject().getName() + " - " + filename;
        }
        return filename;
    }

    private PmdFileInfo determineFileInfo( String filename )
        throws IOException
    {
        File canonicalFilename = new File( filename ).getCanonicalFile();
        PmdFileInfo fileInfo = files.get( canonicalFilename );
        if ( fileInfo == null )
        {
            log.warn( "Couldn't determine PmdFileInfo for file " + filename + " (canonical: " + canonicalFilename
                + "). XRef links won't be available." );
        }

        return fileInfo;
    }

    private void startFileSection( int level, String currentFilename, PmdFileInfo fileInfo )
    {
        sink.section( level, null );
        sink.sectionTitle( level, null );

        // prepare the filename
        this.currentFilename = shortenFilename( currentFilename, fileInfo );

        sink.text( makeFileSectionName( this.currentFilename, fileInfo ) );
        sink.sectionTitle_( level );

        sink.table();
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.pmd.column.rule" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.pmd.column.violation" ) );
        sink.tableHeaderCell_();
        if ( this.renderRuleViolationPriority )
        {
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.pmd.column.priority" ) );
            sink.tableHeaderCell_();
        }
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.pmd.column.line" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();
    }

    private void endFileSection( int level )
    {
        sink.table_();
        sink.section_( level );
    }

    private void processSingleRuleViolation( RuleViolation ruleViolation, PmdFileInfo fileInfo )
    {
        sink.tableRow();
        sink.tableCell();
        sink.link( ruleViolation.getRule().getExternalInfoUrl() );
        sink.text( ruleViolation.getRule().getName() );
        sink.link_();
        sink.tableCell_();
        sink.tableCell();
        sink.text( ruleViolation.getDescription() );
        sink.tableCell_();

        if ( this.renderRuleViolationPriority )
        {
            sink.tableCell();
            sink.text( String.valueOf( ruleViolation.getRule().getPriority().getPriority() ) );
            sink.tableCell_();
        }

        sink.tableCell();

        int beginLine = ruleViolation.getBeginLine();
        outputLineLink( beginLine, fileInfo );
        int endLine = ruleViolation.getEndLine();
        if ( endLine != beginLine )
        {
            sink.text( "&#x2013;" ); // \u2013 is a medium long dash character
            outputLineLink( endLine, fileInfo );
        }

        sink.tableCell_();
        sink.tableRow_();
    }

    // PMD might run the analysis multi-threaded, so the violations might be reported
    // out of order. We sort them here by filename and line number before writing them to
    // the report.
    private void renderViolations()
        throws IOException
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.pmd.files" ) );
        sink.sectionTitle1_();

        // TODO files summary

        List<RuleViolation> violations2 = new ArrayList<>( violations );
        renderViolationsTable( 2, violations2 );

        sink.section1_();
    }

    private void renderViolationsByPriority() throws IOException
    {
        if ( !renderViolationsByPriority )
        {
            return;
        }

        boolean oldPriorityColumn = this.renderRuleViolationPriority;
        this.renderRuleViolationPriority = false;

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.pmd.violationsByPriority" ) );
        sink.sectionTitle1_();

        Map<RulePriority, List<RuleViolation>> violationsByPriority = new HashMap<>();
        for ( RuleViolation violation : violations )
        {
            RulePriority priority = violation.getRule().getPriority();
            List<RuleViolation> violationSegment = violationsByPriority.get( priority );
            if ( violationSegment == null )
            {
                violationSegment = new ArrayList<>();
                violationsByPriority.put( priority, violationSegment );
            }
            violationsByPriority.get( violation.getRule().getPriority() ).add( violation );
        }

        for ( RulePriority priority : RulePriority.values() )
        {
            List<RuleViolation> violationsWithPriority = violationsByPriority.get( priority );
            if ( violationsWithPriority == null || violationsWithPriority.isEmpty() )
            {
                continue;
            }

            sink.section2();
            sink.sectionTitle2();
            sink.text( bundle.getString( "report.pmd.priority" ) + " " + priority.getPriority() );
            sink.sectionTitle2_();

            renderViolationsTable( 3, violationsWithPriority );

            sink.section2_();
        }

        if ( violations.isEmpty() )
        {
            sink.paragraph();
            sink.text( bundle.getString( "report.pmd.noProblems" ) );
            sink.paragraph_();
        }

        sink.section1_();

        this.renderRuleViolationPriority = oldPriorityColumn;
    }

    private void renderViolationsTable( int level, List<RuleViolation> violationSegment )
    throws IOException
    {
        Collections.sort( violationSegment, new Comparator<RuleViolation>()
        {
            /** {@inheritDoc} */
            public int compare( RuleViolation o1, RuleViolation o2 )
            {
                int filenames = o1.getFilename().compareTo( o2.getFilename() );
                if ( filenames == 0 )
                {
                    return o1.getBeginLine() - o2.getBeginLine();
                }
                else
                {
                    return filenames;
                }
            }
        } );

        boolean fileSectionStarted = false;
        String previousFilename = null;
        for ( RuleViolation ruleViolation : violationSegment )
        {
            String currentFn = ruleViolation.getFilename();
            PmdFileInfo fileInfo = determineFileInfo( currentFn );

            if ( !currentFn.equalsIgnoreCase( previousFilename ) && fileSectionStarted )
            {
                endFileSection( level );
                fileSectionStarted = false;
            }
            if ( !fileSectionStarted )
            {
                startFileSection( level, currentFn, fileInfo );
                fileSectionStarted = true;
            }

            processSingleRuleViolation( ruleViolation, fileInfo );

            previousFilename = currentFn;
        }

        if ( fileSectionStarted )
        {
            endFileSection( level );
        }
    }

    private void outputLineLink( int line, PmdFileInfo fileInfo )
    {
        String xrefLocation = null;
        if ( fileInfo != null )
        {
            xrefLocation = fileInfo.getXrefLocation();
        }

        if ( xrefLocation != null )
        {
            sink.link( xrefLocation + "/" + currentFilename.replaceAll( "\\.java$", ".html" ) + "#L" + line );
        }
        sink.text( String.valueOf( line ) );
        if ( xrefLocation != null )
        {
            sink.link_();
        }
    }

    private void processProcessingErrors() throws IOException
    {
        // sort the problem by filename first, since PMD is executed multi-threaded
        // and might reports the results unsorted
        Collections.sort( processingErrors, new Comparator<ProcessingError>()
        {
            @Override
            public int compare( ProcessingError e1, ProcessingError e2 )
            {
                return e1.getFile().compareTo( e2.getFile() );
            }
        } );

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.pmd.processingErrors.title" ) );
        sink.sectionTitle1_();

        sink.table();
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.pmd.processingErrors.column.filename" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.pmd.processingErrors.column.problem" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        for ( ProcessingError error : processingErrors )
        {
            processSingleProcessingError( error );
        }

        sink.table_();

        sink.section1_();
    }

    private void processSingleProcessingError( ProcessingError error ) throws IOException
    {
        String filename = error.getFile();
        PmdFileInfo fileInfo = determineFileInfo( filename );
        filename = makeFileSectionName( shortenFilename( filename, fileInfo ), fileInfo );

        sink.tableRow();
        sink.tableCell();
        sink.text( filename );
        sink.tableCell_();
        sink.tableCell();
        sink.text( error.getMsg() );
        sink.verbatim( null );
        sink.rawText( error.getDetail() );
        sink.verbatim_();
        sink.tableCell_();
        sink.tableRow_();
    }

    public void beginDocument()
    {
        sink.head();
        sink.title();
        sink.text( getTitle() );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( getTitle() );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( bundle.getString( "report.pmd.pmdlink" ) + " " );
        sink.link( "https://pmd.github.io" );
        sink.text( "PMD" );
        sink.link_();
        sink.text( " " + AbstractPmdReport.getPmdVersion() + "." );
        sink.paragraph_();

        sink.section1_();

        // TODO overall summary
    }

    /*
     * private void processMetrics() { if ( metrics.size() == 0 ) { return; } sink.section1(); sink.sectionTitle1();
     * sink.text( "Metrics" ); sink.sectionTitle1_(); sink.table(); sink.tableRow(); sink.tableHeaderCell(); sink.text(
     * "Name" ); sink.tableHeaderCell_(); sink.tableHeaderCell(); sink.text( "Count" ); sink.tableHeaderCell_();
     * sink.tableHeaderCell(); sink.text( "High" ); sink.tableHeaderCell_(); sink.tableHeaderCell(); sink.text( "Low" );
     * sink.tableHeaderCell_(); sink.tableHeaderCell(); sink.text( "Average" ); sink.tableHeaderCell_();
     * sink.tableRow_(); for ( Metric met : metrics ) { sink.tableRow(); sink.tableCell(); sink.text(
     * met.getMetricName() ); sink.tableCell_(); sink.tableCell(); sink.text( String.valueOf( met.getCount() ) );
     * sink.tableCell_(); sink.tableCell(); sink.text( String.valueOf( met.getHighValue() ) ); sink.tableCell_();
     * sink.tableCell(); sink.text( String.valueOf( met.getLowValue() ) ); sink.tableCell_(); sink.tableCell();
     * sink.text( String.valueOf( met.getAverage() ) ); sink.tableCell_(); sink.tableRow_(); } sink.table_();
     * sink.section1_(); }
     */

    public void render()
        throws IOException
    {
        if ( !violations.isEmpty() )
        {
            renderViolationsByPriority();

            renderViolations();
        }
        else
        {
            sink.paragraph();
            sink.text( bundle.getString( "report.pmd.noProblems" ) );
            sink.paragraph_();
        }

        if ( !processingErrors.isEmpty() )
        {
            processProcessingErrors();
        }
    }

    public void endDocument()
        throws IOException
    {
        // The Metrics report useless with the current PMD metrics impl.
        // For instance, run the coupling ruleset and you will get a boatload
        // of excessive imports metrics, none of which is really any use.
        // TODO Determine if we are going to just ignore metrics.

        // processMetrics();

        sink.body_();

        sink.flush();

        sink.close();
    }

    public void setFiles( Map<File, PmdFileInfo> files )
    {
        this.files = files;
    }

    public void setRenderRuleViolationPriority( boolean renderRuleViolationPriority )
    {
        this.renderRuleViolationPriority = renderRuleViolationPriority;
    }

    public void setRenderViolationsByPriority( boolean renderViolationsByPriority )
    {
        this.renderViolationsByPriority = renderViolationsByPriority;
    }
}
