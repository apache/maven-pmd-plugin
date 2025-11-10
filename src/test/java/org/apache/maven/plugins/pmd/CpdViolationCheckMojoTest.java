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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.session.scope.internal.SessionScope;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class CpdViolationCheckMojoTest extends AbstractMojoTestCase {

    private ArtifactStubFactory artifactStubFactory;

    /**
     * Checks whether the string <code>contained</code> is contained in
     * the given <code>text</code>, ignoring case.
     *
     * @param text the string in which the search is executed
     * @param contains the string to be searched for
     * @return <code>true</code> if the text contains the string, otherwise <code>false</code>
     */
    public static boolean lowerCaseContains(String text, String contains) {
        return text.toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT));
    }

    public void testDefaultConfiguration() throws Exception {
        generateReport("cpd", "default-configuration/cpd-default-configuration-plugin-config.xml");

        try {
            File testPom = new File(
                    getBasedir(),
                    "src/test/resources/unit/default-configuration/cpd-check-default-configuration-plugin-config.xml");
            CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo("cpd-check", testPom);
            cpdViolationCheckMojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final MojoFailureException e) {
            assertTrue(e.getMessage().startsWith("CPD " + AbstractPmdReport.getPmdVersion() + " has found 1 duplicat"));
        }
    }

    public void testNotFailOnViolation() throws Exception {
        generateReport("cpd", "default-configuration/cpd-default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/cpd-check-notfailonviolation-plugin-config.xml");
        CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo("cpd-check", testPom);
        cpdViolationCheckMojo.execute();
    }

    public void testException() throws Exception {
        try {
            File testPom = new File(
                    getBasedir(),
                    "src/test/resources/unit/custom-configuration/cpd-check-exception-test-plugin-config.xml");
            CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo("cpd-check", testPom);
            cpdViolationCheckMojo.project = new MavenProject();
            cpdViolationCheckMojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (MojoFailureException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testExclusionsConfiguration() throws Exception {
        generateReport("cpd", "default-configuration/cpd-default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/cpd-check-cpd-exclusions-configuration-plugin-config.xml");
        CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo("cpd-check", testPom);

        // this call shouldn't throw an exception, as the classes with duplications have been excluded
        cpdViolationCheckMojo.execute();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CapturingPrintStream.init(true);

        artifactStubFactory = new DependencyArtifactStubFactory(getTestFile("target"), true, false);
        artifactStubFactory.getWorkingDir().mkdirs();
        SessionScope sessionScope = lookup(SessionScope.class);
        sessionScope.enter();
    }

    @Override
    protected void tearDown() throws Exception {
        SessionScope lookup = lookup(SessionScope.class);
        lookup.exit();
        super.tearDown();
    }

    /**
     * Generate the report and return the generated file.
     *
     * @param goal the mojo goal
     * @param pluginXml the name of the xml file in "src/test/resources/plugin-configs/"
     * @return the generated HTML file
     * @throws Exception if any
     */
    protected File generateReport(String goal, String pluginXml) throws Exception {
        File pluginXmlFile = new File(getBasedir(), "src/test/resources/unit/" + pluginXml);
        AbstractPmdReport mojo = createReportMojo(goal, pluginXmlFile);
        return generateReport(mojo, pluginXmlFile);
    }

    protected AbstractPmdReport createReportMojo(String goal, File pluginXmlFile) throws Exception {
        AbstractPmdReport mojo = lookupMojo(goal, pluginXmlFile);
        assertNotNull("Mojo not found.", mojo);

        SessionScope sessionScope = lookup(SessionScope.class);
        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
        sessionScope.seed(MavenSession.class, mavenSession);

        DefaultRepositorySystemSession repositorySession =
                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));

        List<MavenProject> reactorProjects =
                mojo.getReactorProjects() != null ? mojo.getReactorProjects() : Collections.emptyList();

        setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());
        setVariableValueToObject(mojo, "session", mavenSession);
        setVariableValueToObject(mojo, "repoSession", repositorySession);
        setVariableValueToObject(mojo, "reactorProjects", reactorProjects);
        setVariableValueToObject(
                mojo, "remoteProjectRepositories", mojo.getProject().getRemoteProjectRepositories());
        setVariableValueToObject(
                mojo, "siteDirectory", new File(mojo.getProject().getBasedir(), "src/site"));
        return mojo;
    }

    protected File generateReport(AbstractPmdReport mojo, File pluginXmlFile) throws Exception {
        mojo.execute();

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        return new File(outputDir, filename);
    }

    /**
     * Read the contents of the specified file into a string.
     */
    protected String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    private MojoExecution getMockMojoExecution() {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal("cpd-check");

        MojoExecution execution = new MojoExecution(mojoDescriptor);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-pmd-plugin");
        pluginDescriptor.setPlugin(plugin);
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        return execution;
    }
}
