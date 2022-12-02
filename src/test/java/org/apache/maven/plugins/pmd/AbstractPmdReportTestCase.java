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
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.5
 */
public abstract class AbstractPmdReportTestCase extends AbstractMojoTestCase {
    private ArtifactStubFactory artifactStubFactory;

    /**
     * The current project to be test.
     */
    private MavenProject testMavenProject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CapturingPrintStream.init(true);

        artifactStubFactory = new DependencyArtifactStubFactory(getTestFile("target"), true, false);
        artifactStubFactory.getWorkingDir().mkdirs();
    }

    /**
     * Get the current Maven project
     *
     * @return the maven project
     */
    protected MavenProject getTestMavenProject() {
        return testMavenProject;
    }

    /**
     * Get the generated report as file in the test maven project.
     *
     * @param name the name of the report.
     * @return the generated report as file
     * @throws IOException if the return file doesnt exist
     */
    protected File getGeneratedReport(String name) throws IOException {
        String outputDirectory =
                getBasedir() + "/target/test/unit/" + getTestMavenProject().getArtifactId();

        File report = new File(outputDirectory, name);
        if (!report.exists()) {
            throw new IOException("File not found. Attempted: " + report);
        }

        return report;
    }

    /**
     * Generate the report and return the generated file
     *
     * @param goal the mojo goal.
     * @param pluginXml the name of the xml file in "src/test/resources/plugin-configs/".
     * @return the generated HTML file
     * @throws Exception if any
     */
    protected File generateReport(String goal, String pluginXml) throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/unit/" + pluginXml);
        AbstractPmdReport mojo = createReportMojo(goal, pluginXmlFile);
        return generateReport(mojo, pluginXmlFile);
    }

    protected AbstractPmdReport createReportMojo(String goal, File pluginXmlFile) throws Exception {
        AbstractPmdReport mojo = (AbstractPmdReport) lookupMojo(goal, pluginXmlFile);
        assertNotNull("Mojo not found.", mojo);

        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(newMavenSession(new MavenProjectStub()));
        DefaultRepositorySystemSession repoSession =
                (DefaultRepositorySystemSession) legacySupport.getRepositorySession();
        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repoSession, new LocalRepository(artifactStubFactory.getWorkingDir())));

        List<MavenProject> reactorProjects =
                mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();

        setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());
        setVariableValueToObject(mojo, "session", legacySupport.getSession());
        setVariableValueToObject(mojo, "repoSession", legacySupport.getRepositorySession());
        setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
        setVariableValueToObject(
                mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
        setVariableValueToObject(
                mojo, "siteDirectory", new File(mojo.getProject().getBasedir(), "src/site"));
        return mojo;
    }

    protected File generateReport(AbstractPmdReport mojo, File pluginXmlFile) throws Exception {
        mojo.execute();

        ProjectBuilder builder = lookup(ProjectBuilder.class);

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());

        testMavenProject = builder.build(pluginXmlFile, buildingRequest).getProject();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputName() + ".html";

        return new File(outputDir, filename);
    }

    /**
     * Read the contents of the specified file object into a string
     */
    protected String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * Checks, whether the string <code>contained</code> is contained in
     * the given <code>text</code> ignoring case.
     *
     * @param text the string in which the search is executed
     * @param contains the string, the should be searched
     * @return <code>true</code> if the string is contained, otherwise <code>false</code>.
     */
    public static boolean lowerCaseContains(String text, String contains) {
        return text.toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT));
    }

    private MojoExecution getMockMojoExecution() {
        MojoDescriptor md = new MojoDescriptor();
        md.setGoal(getGoal());

        MojoExecution me = new MojoExecution(md);

        PluginDescriptor pd = new PluginDescriptor();
        Plugin p = new Plugin();
        p.setGroupId("org.apache.maven.plugins");
        p.setArtifactId("maven-pmd-plugin");
        pd.setPlugin(p);
        md.setPluginDescriptor(pd);

        return me;
    }

    protected abstract String getGoal();
}
