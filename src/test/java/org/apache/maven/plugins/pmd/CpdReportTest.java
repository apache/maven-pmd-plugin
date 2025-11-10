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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.session.scope.internal.SessionScope;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class CpdReportTest extends AbstractMojoTestCase {
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
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CapturingPrintStream.init(true);

        artifactStubFactory = new DependencyArtifactStubFactory(getTestFile("target"), true, false);
        artifactStubFactory.getWorkingDir().mkdirs();
        SessionScope sessionScope = lookup(SessionScope.class);
        sessionScope.enter();
        FileUtils.deleteDirectory(new File(getBasedir(), "target/test/unit"));
    }

    /**
     * Test CPDReport given the default configuration
     */
    public void testDefaultConfiguration() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "default-configuration/cpd-default-configuration-plugin-config.xml");
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
     * Test CPDReport with the text renderer given as "format=txt"
     */
    public void testTxtFormat() throws Exception {
        generateReport(getGoal(), "custom-configuration/cpd-txt-format-configuration-plugin-config.xml");

        // check if the CPD files were generated
        File xmlFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/cpd.xml");
        assertTrue(new File(xmlFile.getAbsolutePath()).exists());
        File txtFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/cpd.txt");
        assertTrue(new File(txtFile.getAbsolutePath()).exists());

        // check the contents of cpd.txt
        String str = readFile(txtFile);
        // Contents that should NOT be in the report
        assertFalse(lowerCaseContains(str, "public static void main( String[] args )"));
        // Contents that should be in the report
        assertTrue(lowerCaseContains(str, "public void duplicateMethod( int i )"));
    }

    /**
     * Test CpdReport using custom configuration
     */
    public void testCustomConfiguration() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "custom-configuration/cpd-custom-configuration-plugin-config.xml");
        assertTrue(generatedReport.exists());

        // check if the CPD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/cpd.csv");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedReport);
        // Contents that should NOT be in the report
        assertFalse(lowerCaseContains(str, "/Sample.java"));
        assertFalse(lowerCaseContains(str, "public void duplicateMethod( int i )"));
        // Contents that should be in the report
        assertTrue(lowerCaseContains(str, "AnotherSample.java"));
        assertTrue(lowerCaseContains(str, "public static void main( String[] args )"));
        assertTrue(lowerCaseContains(str, "private String unusedMethod("));
    }

    /**
     * Test CPDReport with invalid format
     */
    public void testInvalidFormat() throws Exception {
        try {
            File testPom = new File(
                    getBasedir(), "src/test/resources/unit/invalid-format/cpd-invalid-format-plugin-config.xml");
            AbstractPmdReport mojo = createReportMojo(getGoal(), testPom);
            setVariableValueToObject(
                    mojo, "compileSourceRoots", mojo.getProject().getCompileSourceRoots());
            generateReport(mojo, testPom);

            // TODO this should be a more specific subclass
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertMavenReportException("Can't find CPD custom format xhtml", e);
        }
    }

    public void testWriteNonHtml() throws Exception {
        generateReport(getGoal(), "default-configuration/cpd-default-configuration-plugin-config.xml");

        // check if the CPD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document pmdCpdDocument = builder.parse(generatedFile);
        assertNotNull(pmdCpdDocument);

        String str = readFile(generatedFile);
        assertTrue(lowerCaseContains(str, "AppSample.java"));
        assertTrue(lowerCaseContains(str, "App.java"));
        assertTrue(lowerCaseContains(str, "public String dup( String str )"));
        assertTrue(lowerCaseContains(str, "tmp = tmp + str.substring( i, i + 1);"));
    }

    /**
     * verify the cpd.xml file is included in the reports when requested.
     *
     * @throws Exception
     */
    public void testIncludeXmlInReports() throws Exception {
        generateReport(getGoal(), "default-configuration/cpd-report-include-xml-in-reports-config.xml");

        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document pmdCpdDocument = builder.parse(generatedFile);
        assertNotNull(pmdCpdDocument);

        String str = readFile(generatedFile);
        assertTrue(str.contains("</pmd-cpd>"));

        File siteReport = new File(getBasedir(), "target/test/unit/default-configuration/target/site/cpd.xml");
        assertTrue(new File(siteReport.getAbsolutePath()).exists());
        String siteReportContent = readFile(siteReport);
        assertTrue(siteReportContent.contains("</pmd-cpd>"));
        assertEquals(str, siteReportContent);
    }

    public void testSkipEmptyReportConfiguration() throws Exception {
        // verify the generated files do not exist because PMD was skipped
        File generatedReport = generateReport(getGoal(), "empty-report/cpd-skip-empty-report-plugin-config.xml");
        assertFalse(new File(generatedReport.getAbsolutePath()).exists());
    }

    public void testEmptyReportConfiguration() throws Exception {
        // verify the generated files do exist, even if there are no violations
        File generatedReport = generateReport(getGoal(), "empty-report/cpd-empty-report-plugin-config.xml");
        assertTrue(
                generatedReport.getAbsolutePath() + " does not exist",
                new File(generatedReport.getAbsolutePath()).exists());

        String str = readFile(generatedReport);
        assertFalse(lowerCaseContains(str, "Hello.java"));
        assertTrue(str.contains("CPD found no problems in your source code."));
    }

    public void testCpdEncodingConfiguration() throws Exception {
        String originalEncoding = System.getProperty("file.encoding");
        try {
            System.setProperty("file.encoding", "UTF-16");

            generateReport(getGoal(), "default-configuration/cpd-default-configuration-plugin-config.xml");

            // check if the CPD files were generated
            File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
            assertTrue(generatedFile.exists());
            String str = readFile(generatedFile);
            assertTrue(lowerCaseContains(str, "AppSample.java"));
        } finally {
            System.setProperty("file.encoding", originalEncoding);
        }
    }

    public void testCpdJavascriptConfiguration() throws Exception {
        generateReport(getGoal(), "default-configuration/cpd-javascript-plugin-config.xml");

        // verify the generated file exists and violations are reported
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);
        assertTrue(lowerCaseContains(str, "Sample.js"));
        assertTrue(lowerCaseContains(str, "SampleDup.js"));
    }

    public void testCpdJspConfiguration() throws Exception {
        generateReport(getGoal(), "default-configuration/cpd-jsp-plugin-config.xml");

        // verify the generated file exists and violations are reported
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);
        assertTrue(lowerCaseContains(str, "sample.jsp"));
        assertTrue(lowerCaseContains(str, "sampleDup.jsp"));
    }

    public void testExclusionsConfiguration() throws Exception {
        generateReport(getGoal(), "default-configuration/cpd-report-cpd-exclusions-configuration-plugin-config.xml");

        // verify the generated file exists and no duplications are reported
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);
        assertEquals(0, StringUtils.countMatches(str, "<duplication"));
    }

    public void testWithCpdErrors() throws Exception {
        try {
            generateReport(getGoal(), "CpdReportTest/with-cpd-errors/pom.xml");

            fail("MojoExecutionException must be thrown");
        } catch (MojoExecutionException e) {
            assertMavenReportException("There was 1 error while executing CPD", e);
            assertReportContains("Lexical error in file");
            assertReportContains("BadFile.java");
        }
    }

    private static void assertMavenReportException(String expectedMessage, Exception exception) {
        MavenReportException cause = (MavenReportException) exception.getCause();
        String message = cause.getMessage();
        assertTrue(
                "Wrong message: expected: " + expectedMessage + ", but was: " + message,
                message.contains(expectedMessage));
    }

    private static void assertReportContains(String expectedMessage) throws IOException {
        Path path = Paths.get(getBasedir(), "target/test/unit/CpdReportTest/with-cpd-errors/target/cpd.xml");
        String report = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(
                "Expected '" + expectedMessage + "' in cpd.xml, but was:\n" + report, report.contains(expectedMessage));
    }

    protected String getGoal() {
        return "cpd";
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
        mojoDescriptor.setGoal(getGoal());

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
