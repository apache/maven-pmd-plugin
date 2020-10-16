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
import java.util.List;

import org.apache.maven.plugins.pmd.model.CpdErrorDetail;
import org.apache.maven.plugins.pmd.model.Duplication;
import org.apache.maven.plugins.pmd.model.io.xpp3.CpdXpp3Reader;
import org.apache.maven.reporting.MavenReportException;

/**
 * Provides access to the result of the CPD analysis.
 */
public class CpdResult
{
    private final List<Duplication> duplications = new ArrayList<>();

    public CpdResult( File report, String encoding ) throws MavenReportException
    {
        loadResult( report, encoding );
    }

    public List<Duplication> getDuplications()
    {
        return duplications;
    }

    public boolean hasDuplications()
    {
        return !duplications.isEmpty();
    }

    private void loadResult( File report, String encoding ) throws MavenReportException
    {
        try ( Reader reader1 = new InputStreamReader( new FileInputStream( report ), encoding ) )
        {
            CpdXpp3Reader reader = new CpdXpp3Reader();
            CpdErrorDetail details = reader.read( reader1, false );
            duplications.addAll( details.getDuplications() );
        }
        catch ( Exception e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
    }
}
