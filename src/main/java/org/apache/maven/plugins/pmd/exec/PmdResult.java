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
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugins.pmd.model.PmdErrorDetail;
import org.apache.maven.plugins.pmd.model.PmdFile;
import org.apache.maven.plugins.pmd.model.ProcessingError;
import org.apache.maven.plugins.pmd.model.SuppressedViolation;
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
    private final List<SuppressedViolation> suppressedViolations = new ArrayList<>();

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
        try ( Reader reader1 = new BomFilter( encoding, new InputStreamReader(
                new FileInputStream( pmdFile ), encoding ) ) )
        {
            PmdXpp3Reader reader = new PmdXpp3Reader();
            PmdErrorDetail details = reader.read( reader1, false );
            processingErrors.addAll( details.getErrors() );
            suppressedViolations.addAll( details.getSuppressedViolations() );

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

    // Note: This seems to be a bug in PMD's XMLRenderer. The BOM is rendered multiple times.
    // once at the beginning of the file, which is Ok, but also in the middle of the file.
    // This filter just skips all BOMs if the encoding is not UTF-8
    private static class BomFilter extends FilterReader
    {
        private static final char BOM = '\uFEFF';
        private final boolean filter;

        BomFilter( String encoding, Reader in )
        {
            super( in );
            filter = !"UTF-8".equalsIgnoreCase( encoding );
        }

        @Override
        public int read() throws IOException
        {
            int c = super.read();

            if ( !filter )
            {
                return c;
            }

            while ( c != -1 && c == BOM )
            {
                c = super.read();
            }
            return c;
        }

        @Override
        public int read( char[] cbuf, int off, int len ) throws IOException
        {
            int count = super.read( cbuf, off, len );

            if ( !filter )
            {
                return count;
            }

            if ( count != -1 )
            {
                for ( int i = off; i < off + count; i++ )
                {
                    if ( cbuf[i] == BOM )
                    {
                        // shift the content one char to the left
                        System.arraycopy( cbuf, i + 1, cbuf, i, off + count - 1 - i );
                        count--;
                    }
                }
            }
            return count;
        }
    }

    public Collection<Violation> getViolations()
    {
        return violations;
    }

    public Collection<SuppressedViolation> getSuppressedViolations()
    {
        return suppressedViolations;
    }

    public Collection<ProcessingError> getErrors()
    {
        return processingErrors;
    }
}
