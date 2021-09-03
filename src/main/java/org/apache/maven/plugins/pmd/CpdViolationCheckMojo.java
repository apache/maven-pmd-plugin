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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.pmd.model.CpdErrorDetail;
import org.apache.maven.plugins.pmd.model.CpdFile;
import org.apache.maven.plugins.pmd.model.Duplication;
import org.apache.maven.plugins.pmd.model.io.xpp3.CpdXpp3Reader;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Fails the build if there were any CPD violations in the source code.
 *
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "cpd-check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true )
@Execute( goal = "cpd" )
public class CpdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo<Duplication>
{
    /**
     * Default constructor. Initializes with the correct {@link ExcludeDuplicationsFromFile}.
     */
    public CpdViolationCheckMojo()
    {
        super( new ExcludeDuplicationsFromFile() );
    }

    /**
     * Skip the CPD violation checks. Most useful on the command line via "-Dcpd.skip=true".
     */
    @Parameter( property = "cpd.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Whether to fail the build if the validation check fails.
     *
     * @since 3.0
     */
    @Parameter( property = "cpd.failOnViolation", defaultValue = "true", required = true )
    protected boolean failOnViolation;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Skipping CPD execution" );
            return;
        }

        executeCheck( "cpd.xml", "duplication", "CPD duplication", 10 );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void printError( Duplication item, String severity )
    {
        int lines = item.getLines();

        StringBuilder buff = new StringBuilder( 100 );
        buff.append( "CPD " ).append( severity ).append( ": Found " );
        buff.append( lines ).append( " lines of duplicated code at locations:" );
        this.getLog().info( buff.toString() );

        for ( CpdFile file : item.getFiles() )
        {
            buff.setLength( 0 );
            buff.append( "    " );
            buff.append( file.getPath() );
            buff.append( " line " ).append( file.getLine() );
            this.getLog().info( buff.toString() );
        }

        this.getLog().debug( "CPD " + severity + ": Code Fragment " );
        this.getLog().debug( item.getCodefragment() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Duplication> getErrorDetails( File cpdFile )
        throws XmlPullParserException, IOException
    {
        try ( FileReader fileReader = new FileReader( cpdFile ) )
        {
            CpdXpp3Reader reader = new CpdXpp3Reader();
            CpdErrorDetail details = reader.read( fileReader, false );
            return details.getDuplications();
        }
    }

    @Override
    protected int getPriority( Duplication errorDetail )
    {
        return 0;
    }

    @Override
    protected ViolationDetails<Duplication> newViolationDetailsInstance()
    {
        return new ViolationDetails<>();
    }

    @Override
    public boolean isFailOnViolation()
    {
        return failOnViolation;
    }
}