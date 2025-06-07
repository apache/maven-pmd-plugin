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
package org.apache.maven.plugins.pmd.stubs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class DefaultConfigurationMavenProjectStub extends PmdProjectStub {
    private List<ReportPlugin> reportPlugins = new ArrayList<>();

    private Build build;

    public DefaultConfigurationMavenProjectStub() throws XmlPullParserException, IOException {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();

        InputStream is = new FileInputStream(getBasedir() + "/" + getPOM());
        Model model = pomReader.read(is);

        setGroupId(model.getGroupId());
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setName(model.getName());
        setUrl(model.getUrl());
        setPackaging(model.getPackaging());
        setReportPlugins(model.getReporting().getPlugins());

        Build build = new Build();
        build.setFinalName(model.getBuild().getFinalName());
        build.setDirectory(getBasedir() + "/target");
        build.setSourceDirectory(getBasedir().getAbsolutePath());
        setBuild(build);

        String basedir = getBasedir().getAbsolutePath();
        List<String> compileSourceRoots = new ArrayList<>();
        compileSourceRoots.add(basedir + "/def/configuration");
        setCompileSourceRoots(compileSourceRoots);

        File file = new File(getBasedir().getAbsolutePath() + "/pom.xml");
        setFile(file);

        Artifact artifact = new PmdPluginArtifactStub(getGroupId(), getArtifactId(), getVersion(), getPackaging());
        artifact.setArtifactHandler(new DefaultArtifactHandlerStub());
        setArtifact(artifact);
    }

    public void setReportPlugins(List<ReportPlugin> plugins) {
        this.reportPlugins = plugins;
    }

    /** {@inheritDoc} */
    @Override
    public List<ReportPlugin> getReportPlugins() {
        return reportPlugins;
    }

    /** {@inheritDoc} */
    @Override
    public void setBuild(Build build) {
        this.build = build;
    }

    /** {@inheritDoc} */
    @Override
    public Build getBuild() {
        return build;
    }

    @Override
    public File getBasedir() {
        return new File(super.getBasedir() + "/default-configuration");
    }

    @Override
    protected String getPOM() {
        return "default-configuration-plugin-config.xml";
    }
}
