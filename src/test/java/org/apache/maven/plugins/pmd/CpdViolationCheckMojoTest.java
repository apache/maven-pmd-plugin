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
import java.util.Collections;
import java.util.List;

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
        AbstractPmdReport mojo1 = lookupMojo(goal, pluginXmlFile);
        assertNotNull("Mojo not found.", mojo1);

        SessionScope sessionScope = lookup(SessionScope.class);
        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
        sessionScope.seed(MavenSession.class, mavenSession);

        DefaultRepositorySystemSession repositorySession =
                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));

        List<MavenProject> reactorProjects =
                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();

        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
        setVariableValueToObject(mojo1, "session", mavenSession);
        setVariableValueToObject(mojo1, "repoSession", repositorySession);
        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
        setVariableValueToObject(
                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
        setVariableValueToObject(
                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
        AbstractPmdReport mojo = mojo1;
        mojo.execute();

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        return new File(outputDir, filename);
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
