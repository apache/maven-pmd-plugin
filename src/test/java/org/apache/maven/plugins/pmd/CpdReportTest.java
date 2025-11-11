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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.reporting.MavenReportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
@MojoTest
public class CpdReportTest {

    @Inject
    private MavenSession mavenSession;

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

    /**
     * {@inheritDoc}
     */
    @BeforeEach
    public void setUp() throws Exception {
        //        CapturingPrintStream.init(true);
        //
        //        artifactStubFactory = new DependencyArtifactStubFactory(getTestFile("target"), true, false);
        //        artifactStubFactory.getWorkingDir().mkdirs();
        //        SessionScope sessionScope = lookup(SessionScope.class);
        //        sessionScope.enter();
        //        FileUtils.deleteDirectory(new File(getBasedir(), "target/test/unit"));
    }

    /**
     * Test CPDReport given the default configuration
     */
    @InjectMojo(
            goal = "cpd",
            pom = "src/test/resources/unit/default-configuration/cpd-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfiguration(CpdReport mojo) throws Exception {
        //        SessionScope sessionScope = lookup(SessionScope.class);
        //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
        //        sessionScope.seed(MavenSession.class, mavenSession);
        //
        //        DefaultRepositorySystemSession repositorySession =
        //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
        //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
        //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
        //
        //        List<MavenProject> reactorProjects =
        //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
        //
        //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
        //        setVariableValueToObject(mojo1, "session", mavenSession);
        //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
        //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
        //        setVariableValueToObject(
        //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
        //        mojo.execute();

        //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
        //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(new File(generatedReport.getAbsolutePath()).exists());

        // check if the CPD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());

        // check the contents of cpd.html
        String str = readFile(generatedReport);
        assertTrue(lowerCaseContains(str, "AppSample.java"));
        assertTrue(lowerCaseContains(str, "App.java"));
        assertTrue(lowerCaseContains(str, "public String dup( String str )"));
        assertTrue(lowerCaseContains(str, "tmp = tmp + str.substring( i, i + 1);"));
    }

    /**
     * //     * Test CPDReport with the text renderer given as "format=txt"
     * //     */
    //    @Test
    //    public void testTxtFormat(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        // check if the CPD files were generated
    //        File xmlFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/cpd.xml");
    //        assertTrue(new File(xmlFile.getAbsolutePath()).exists());
    //        File txtFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/cpd.txt");
    //        assertTrue(new File(txtFile.getAbsolutePath()).exists());
    //
    //        // check the contents of cpd.txt
    //        String str = readFile(txtFile);
    //        // Contents that should NOT be in the report
    //        assertFalse(lowerCaseContains(str, "public static void main( String[] args )"));
    //        // Contents that should be in the report
    //        assertTrue(lowerCaseContains(str, "public void duplicateMethod( int i )"));
    //    }
    //
    //    /**
    //     * Test CpdReport using custom configuration
    //     */
    //    @Test
    //    public void testCustomConfiguration(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        File outputDir = mojo.getReportOutputDirectory();
    //        String filename = mojo.getOutputPath() + ".html";
    //
    //        File generatedReport = new File(outputDir, filename);
    //        assertTrue(generatedReport.exists());
    //
    //        // check if the CPD files were generated
    //        File generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/cpd.csv");
    //        assertTrue(generatedFile.exists());
    //
    //        String str = readFile(generatedReport);
    //        // Contents that should NOT be in the report
    //        assertFalse(lowerCaseContains(str, "/Sample.java"));
    //        assertFalse(lowerCaseContains(str, "public void duplicateMethod( int i )"));
    //        // Contents that should be in the report
    //        assertTrue(lowerCaseContains(str, "AnotherSample.java"));
    //        assertTrue(lowerCaseContains(str, "public static void main( String[] args )"));
    //        assertTrue(lowerCaseContains(str, "private String unusedMethod("));
    //    }
    //
    //    /**
    //     * Test CPDReport with invalid format
    //     */
    //    @Test
    //    public void testInvalidFormat() throws Exception {
    //        try {
    //            File testPom = new File(
    //                    getBasedir(), "src/test/resources/unit/invalid-format/cpd-invalid-format-plugin-config.xml");
    //            assertNotNull(mojo1, "Mojo not found.");
    //
    //            SessionScope sessionScope = lookup(SessionScope.class);
    //            MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //            sessionScope.seed(MavenSession.class, mavenSession);
    //
    //            DefaultRepositorySystemSession repositorySession =
    //                    (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //            repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                    .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //            List<MavenProject> reactorProjects =
    //                    mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //            setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //            setVariableValueToObject(mojo1, "session", mavenSession);
    //            setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //            setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //            setVariableValueToObject(
    //                    mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //            setVariableValueToObject(
    //                    mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //            AbstractPmdReport mojo = mojo1;
    //            mojo.execute();
    //
    //            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //            buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //            // TODO this should be a more specific subclass
    //            fail("RuntimeException must be thrown");
    //        } catch (RuntimeException e) {
    //            assertMavenReportException("Can't find CPD custom format xhtml", e);
    //        }
    //    }
    //
    //    @Test
    //    public void testWriteNonHtml(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        // check if the CPD files were generated
    //        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
    //        assertTrue(generatedFile.exists());
    //
    //        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    //        Document pmdCpdDocument = builder.parse(generatedFile);
    //        assertNotNull(pmdCpdDocument);
    //
    //        String str = readFile(generatedFile);
    //        assertTrue(lowerCaseContains(str, "AppSample.java"));
    //        assertTrue(lowerCaseContains(str, "App.java"));
    //        assertTrue(lowerCaseContains(str, "public String dup( String str )"));
    //        assertTrue(lowerCaseContains(str, "tmp = tmp + str.substring( i, i + 1);"));
    //    }
    //
    //    /**
    //     * verify the cpd.xml file is included in the reports when requested.
    //     *
    //     * @throws Exception
    //     */
    //    @Test
    //    public void testIncludeXmlInReports(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
    //        assertTrue(generatedFile.exists());
    //
    //        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    //        Document pmdCpdDocument = builder.parse(generatedFile);
    //        assertNotNull(pmdCpdDocument);
    //
    //        String str = readFile(generatedFile);
    //        assertTrue(str.contains("</pmd-cpd>"));
    //
    //        File siteReport = new File(getBasedir(), "target/test/unit/default-configuration/target/site/cpd.xml");
    //        assertTrue(new File(siteReport.getAbsolutePath()).exists());
    //        String siteReportContent = readFile(siteReport);
    //        assertTrue(siteReportContent.contains("</pmd-cpd>"));
    //        assertEquals(str, siteReportContent);
    //    }
    //
    //    @Test
    //    public void testSkipEmptyReportConfiguration(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        File outputDir = mojo.getReportOutputDirectory();
    //        String filename = mojo.getOutputPath() + ".html";
    //
    //        File generatedReport = new File(outputDir, filename);
    //        assertFalse(new File(generatedReport.getAbsolutePath()).exists());
    //    }
    //
    //    @Test
    //    public void testEmptyReportConfiguration(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        File outputDir = mojo.getReportOutputDirectory();
    //        String filename = mojo.getOutputPath() + ".html";
    //
    //        File generatedReport = new File(outputDir, filename);
    //        assertTrue(
    //                new File(generatedReport.getAbsolutePath()).exists(),
    //                generatedReport.getAbsolutePath() + " does not exist");
    //
    //        String str = readFile(generatedReport);
    //        assertFalse(lowerCaseContains(str, "Hello.java"));
    //        assertTrue(str.contains("CPD found no problems in your source code."));
    //    }
    //
    //    @Test
    //    public void testCpdEncodingConfiguration() throws Exception {
    //        String originalEncoding = System.getProperty("file.encoding");
    //        try {
    //            System.setProperty("file.encoding", "UTF-16");
    //
    //            File pluginXmlFile = new File(
    //                    getBasedir(),
    //                    "src/test/resources/unit/" +
    // "default-configuration/cpd-default-configuration-plugin-config.xml");
    //            assertNotNull(mojo1, "Mojo not found.");
    //
    //            SessionScope sessionScope = lookup(SessionScope.class);
    //            MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //            sessionScope.seed(MavenSession.class, mavenSession);
    //
    //            DefaultRepositorySystemSession repositorySession =
    //                    (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //            repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                    .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //            List<MavenProject> reactorProjects =
    //                    mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //            setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //            setVariableValueToObject(mojo1, "session", mavenSession);
    //            setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //            setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //            setVariableValueToObject(
    //                    mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //            setVariableValueToObject(
    //                    mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //            AbstractPmdReport mojo = mojo1;
    //            mojo.execute();
    //
    //            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //            buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //            // check if the CPD files were generated
    //            File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
    //            assertTrue(generatedFile.exists());
    //            String str = readFile(generatedFile);
    //            assertTrue(lowerCaseContains(str, "AppSample.java"));
    //        } finally {
    //            System.setProperty("file.encoding", originalEncoding);
    //        }
    //    }
    //
    //    @Test
    //    public void testCpdJavascriptConfiguration(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        // verify the generated file exists and violations are reported
    //        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
    //        assertTrue(generatedFile.exists());
    //        String str = readFile(generatedFile);
    //        assertTrue(lowerCaseContains(str, "Sample.js"));
    //        assertTrue(lowerCaseContains(str, "SampleDup.js"));
    //    }
    //
    //    @Test
    //    public void testCpdJspConfiguration(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        // verify the generated file exists and violations are reported
    //        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
    //        assertTrue(generatedFile.exists());
    //        String str = readFile(generatedFile);
    //        assertTrue(lowerCaseContains(str, "sample.jsp"));
    //        assertTrue(lowerCaseContains(str, "sampleDup.jsp"));
    //    }
    //
    //    @Test
    //    public void testExclusionsConfiguration(AbstractPmdReport mojo1) throws Exception {
    //        assertNotNull(mojo1, "Mojo not found.");
    //
    //        SessionScope sessionScope = lookup(SessionScope.class);
    //        MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //        sessionScope.seed(MavenSession.class, mavenSession);
    //
    //        DefaultRepositorySystemSession repositorySession =
    //                (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //        repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //        List<MavenProject> reactorProjects =
    //                mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //        setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //        setVariableValueToObject(mojo1, "session", mavenSession);
    //        setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //        setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //        setVariableValueToObject(
    //                mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //        setVariableValueToObject(
    //                mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //        AbstractPmdReport mojo = mojo1;
    //        mojo.execute();
    //
    //        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //        buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //        // verify the generated file exists and no duplications are reported
    //        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
    //        assertTrue(generatedFile.exists());
    //        String str = readFile(generatedFile);
    //        assertEquals(0, StringUtils.countMatches(str, "<duplication"));
    //    }
    //
    //    @Test
    //    public void testWithCpdErrors() throws Exception {
    //        try {
    //            File pluginXmlFile =
    //                    new File(getBasedir(), "src/test/resources/unit/" + "CpdReportTest/with-cpd-errors/pom.xml");
    //            assertNotNull(mojo1, "Mojo not found.");
    //
    //            SessionScope sessionScope = lookup(SessionScope.class);
    //            MavenSession mavenSession = newMavenSession(new MavenProjectStub());
    //            sessionScope.seed(MavenSession.class, mavenSession);
    //
    //            DefaultRepositorySystemSession repositorySession =
    //                    (DefaultRepositorySystemSession) mavenSession.getRepositorySession();
    //            repositorySession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory()
    //                    .newInstance(repositorySession, new LocalRepository(artifactStubFactory.getWorkingDir())));
    //
    //            List<MavenProject> reactorProjects =
    //                    mojo1.getReactorProjects() != null ? mojo1.getReactorProjects() : Collections.emptyList();
    //
    //            setVariableValueToObject(mojo1, "mojoExecution", getMockMojoExecution());
    //            setVariableValueToObject(mojo1, "session", mavenSession);
    //            setVariableValueToObject(mojo1, "repoSession", repositorySession);
    //            setVariableValueToObject(mojo1, "reactorProjects", reactorProjects);
    //            setVariableValueToObject(
    //                    mojo1, "remoteProjectRepositories", mojo1.getProject().getRemoteProjectRepositories());
    //            setVariableValueToObject(
    //                    mojo1, "siteDirectory", new File(mojo1.getProject().getBasedir(), "src/site"));
    //            AbstractPmdReport mojo = mojo1;
    //            mojo.execute();
    //
    //            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest();
    //            buildingRequest.setRepositorySession(lookup(LegacySupport.class).getRepositorySession());
    //
    //            fail("MojoExecutionException must be thrown");
    //        } catch (MojoExecutionException e) {
    //            assertMavenReportException("There was 1 error while executing CPD", e);
    //            assertReportContains("Lexical error in file");
    //            assertReportContains("BadFile.java");
    //        }
    //    }

    private static void assertMavenReportException(String expectedMessage, Exception exception) {
        MavenReportException cause = (MavenReportException) exception.getCause();
        String message = cause.getMessage();
        assertTrue(
                message.contains(expectedMessage),
                "Wrong message: expected: " + expectedMessage + ", but was: " + message);
    }

    private static void assertReportContains(String expectedMessage) throws IOException {
        Path path = Paths.get(getBasedir(), "target/test/unit/CpdReportTest/with-cpd-errors/target/cpd.xml");
        String report = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(
                report.contains(expectedMessage), "Expected '" + expectedMessage + "' in cpd.xml, but was:\n" + report);
    }

    @AfterEach
    public void tearDown() throws Exception {
        //        SessionScope lookup = lookup(SessionScope.class);
        //        lookup.exit();
    }

    /**
     * Read the contents of the specified file into a string.
     */
    protected String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    private MojoExecution getMockMojoExecution() {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal("cpd");

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
