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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugins.pmd.model.PmdErrorDetail;
import org.apache.maven.plugins.pmd.model.PmdFile;
import org.apache.maven.plugins.pmd.model.ProcessingError;
import org.apache.maven.plugins.pmd.model.Violation;
import org.apache.maven.plugins.pmd.model.io.xpp3.PmdXpp3Reader;
import org.apache.maven.reporting.MavenReportException;

/**
 * Provides access to the result of the pmd analysis.
 */
public class PmdResult
{
    private final List<ProcessingError> processingErrors = new ArrayList<>();
    private final List<Violation> violations = new ArrayList<>();

    public static final PmdResult EMPTY = new PmdResult();

    private PmdResult()
    {
    }

    public PmdResult( File pmdFile, String encoding ) throws MavenReportException
    {
        loadResult( pmdFile, encoding );
    }

    public boolean hasViolations()
    {
        return !violations.isEmpty();
    }

    private void loadResult( File pmdFile, String encoding ) throws MavenReportException
    {
        try ( Reader reader1 = new InputStreamReader( new FileInputStream( pmdFile ), encoding ) )
        {
            PmdXpp3Reader reader = new PmdXpp3Reader();
            PmdErrorDetail details = reader.read( reader1, false );
            processingErrors.addAll( details.getErrors() );

            for ( PmdFile file : details.getFiles() )
            {
                String filename = file.getName();
                for ( Violation violation : file.getViolations() )
                {
                    violation.setFileName( filename );
                    violations.add( violation );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
    }

    public Collection<Violation> getViolations()
    {
        return violations;
    }

    public Collection<ProcessingError> getErrors()
    {
        return processingErrors;
    }
}
