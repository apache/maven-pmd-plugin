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
package org.apache.maven.plugins.pmd;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;

/**
 * @version $Id$
 */
public class PmdFileInfo {

    private MavenProject project;

    private File sourceDir;

    private String xref;

    public PmdFileInfo(MavenProject project, File dir, String x) throws IOException {
        this.project = project;
        if (dir.isAbsolute()) {
            this.sourceDir = dir.getCanonicalFile();
        } else {
            this.sourceDir = new File(project.getBasedir(), dir.getPath()).getCanonicalFile();
        }
        this.xref = x;
    }

    public String getXrefLocation() {
        return xref;
    }

    public File getSourceDirectory() {
        return sourceDir;
    }

    public MavenProject getProject() {
        return project;
    }
}
