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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.testing.PlexusExtension;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
@MojoTest
public class CpdReportTest {

    @Inject
    private MavenSession mavenSession;

    @Inject
    private DefaultRepositorySystemSessionFactory repoSessionFactory;

    @Inject
    private MavenProject testMavenProject;

    @Inject
    private MojoExecution mojoExecution;

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
    public void setUp() {
        ArtifactRepository localRepo = Mockito.mock(ArtifactRepository.class);
        Mockito.when(localRepo.getBasedir())
                .thenReturn(new File(PlexusExtension.getBasedir(), "target/local-repo").getAbsolutePath());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(localRepo);

        RemoteRepository centralRepo =
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

        DefaultRepositorySystemSession systemSession = repoSessionFactory.newRepositorySession(request);
        Mockito.when(mavenSession.getRepositorySession()).thenReturn(systemSession);
        Mockito.when(testMavenProject.getRemoteProjectRepositories())
                .thenReturn(Collections.singletonList(centralRepo));

        Mockito.when(mojoExecution.getPlugin()).thenReturn(new Plugin());
    }

    /**
     * Test CPDReport given the default configuration
     */
    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfiguration(CpdReport mojo) throws Exception {
        mojo.execute();

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
    @Basedir("/unit/custom-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-txt-format-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testTxtFormat(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

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
    @Basedir("/unit/custom-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-custom-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testCustomConfiguration(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
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
    @Basedir("/unit/invalid-format")
    @InjectMojo(goal = "cpd", pom = "cpd-invalid-format-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testInvalidFormat(AbstractPmdReport mojo) {
        try {
            mojo.execute();

            // TODO should have more specific exception
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertMavenReportException("Can't find CPD custom format xhtml", e);
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testWriteNonHtml(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

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
    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-report-include-xml-in-reports-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testIncludeXmlInReports(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

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

    @Basedir("/unit/empty-report")
    @InjectMojo(goal = "cpd", pom = "cpd-skip-empty-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSkipEmptyReportConfiguration(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertFalse(new File(generatedReport.getAbsolutePath()).exists());
    }

    @Basedir("/unit/empty-report")
    @InjectMojo(goal = "cpd", pom = "cpd-empty-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testEmptyReportConfiguration(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(
                new File(generatedReport.getAbsolutePath()).exists(),
                generatedReport.getAbsolutePath() + " does not exist");

        String str = readFile(generatedReport);
        assertFalse(lowerCaseContains(str, "Hello.java"));
        assertTrue(str.contains("CPD found no problems in your source code."));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testCpdEncodingConfiguration(AbstractPmdReport mojo) throws Exception {
        String originalEncoding = System.getProperty("file.encoding");
        try {
            System.setProperty("file.encoding", "UTF-16");

            mojo.execute();

            // check if the CPD files were generated
            File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
            assertTrue(generatedFile.exists());
            String str = readFile(generatedFile);
            assertTrue(lowerCaseContains(str, "AppSample.java"));
        } finally {
            System.setProperty("file.encoding", originalEncoding);
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-javascript-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testCpdJavascriptConfiguration(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

        // verify the generated file exists and violations are reported
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);
        assertTrue(lowerCaseContains(str, "Sample.js"));
        assertTrue(lowerCaseContains(str, "SampleDup.js"));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-jsp-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testCpdJspConfiguration(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

        // verify the generated file exists and violations are reported
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);
        assertTrue(lowerCaseContains(str, "sample.jsp"));
        assertTrue(lowerCaseContains(str, "sampleDup.jsp"));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd", pom = "cpd-report-cpd-exclusions-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testExclusionsConfiguration(AbstractPmdReport mojo) throws Exception {
        mojo.execute();

        // verify the generated file exists and no duplications are reported
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/cpd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);
        assertEquals(0, StringUtils.countMatches(str, "<duplication"));
    }

    @Basedir("/unit/CpdReportTest")
    @InjectMojo(goal = "cpd", pom = "with-cpd-errors/pom.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testWithCpdErrors(AbstractPmdReport mojo) throws Exception {
        try {
            mojo.execute();

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
                message.contains(expectedMessage),
                "Wrong message: expected: " + expectedMessage + ", but was: " + message);
    }

    private static void assertReportContains(String expectedMessage) throws IOException {
        Path path = Paths.get(getBasedir(), "target/test/unit/CpdReportTest/with-cpd-errors/target/cpd.xml");
        String report = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(
                report.contains(expectedMessage), "Expected '" + expectedMessage + "' in cpd.xml, but was:\n" + report);
    }

    /**
     * Read the contents of the specified file into a string.
     */
    protected String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
