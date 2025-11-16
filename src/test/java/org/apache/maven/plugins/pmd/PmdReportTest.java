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
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.sourceforge.pmd.renderers.Renderer;
import org.apache.commons.io.IOUtils;
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
import org.apache.maven.plugins.pmd.exec.PmdExecutor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.testing.PlexusExtension;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
public class PmdReportTest {

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
    public void setUp() throws Exception {
        CapturingPrintStream.init(true);
        ArtifactRepository localRepo = Mockito.mock(ArtifactRepository.class);
        Mockito.when(localRepo.getBasedir())
                .thenReturn(new File(PlexusExtension.getBasedir(), "target/local-repo").getAbsolutePath());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(localRepo);

        RemoteRepository centralRepo =
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

        DefaultRepositorySystemSession systemSession = repoSessionFactory.newRepositorySession(request);
        Mockito.when(mavenSession.getRepositorySession()).thenReturn(systemSession);
        Mockito.when(mavenSession.getRequest()).thenReturn(request);
        Mockito.when(testMavenProject.getRemoteProjectRepositories())
                .thenReturn(Collections.singletonList(centralRepo));

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-pmd-plugin");
        Mockito.when(mojoExecution.getPlugin()).thenReturn(plugin);
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // check if the rulesets, that have been applied, have been copied
        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/001-maven-pmd-plugin-default.xml");
        assertTrue(generatedFile.exists());

        // check if there's a link to the JXR files
        String str = readFile(generatedReport);

        assertTrue(str.contains("/xref/def/configuration/App.html#L31"));

        assertTrue(str.contains("/xref/def/configuration/AppSample.html#L45"));

        // check if there's a priority column
        assertTrue(str.contains("<th>Priority</th>"));

        // there should be a rule column
        assertTrue(str.contains("<th>Rule</th>"));
        // along with a link to the rule
        assertTrue(str.contains("pmd_rules_java_bestpractices.html#unusedprivatefield\">UnusedPrivateField</a>"));

        // there should be the section Violations By Priority
        assertTrue(str.contains("Violations By Priority</h2>"));
        assertTrue(str.contains("Priority 3</h3>"));
        assertTrue(str.contains("Priority 4</h3>"));
        // the file App.java is mentioned 3 times: in prio 3, in prio 4 and in the files section
        assertEquals(3, StringUtils.countMatches(str, "def/configuration/App.java"));

        // there must be no warnings (like deprecated rules) in the log output
        String output = CapturingPrintStream.getOutput();
        assertFalse(output.contains("deprecated Rule name"));
        assertFalse(output.contains("Discontinue using Rule name"));
        assertFalse(output.contains("is referenced multiple times"));

        // the version should be logged
        assertTrue(output.contains("PMD version: " + AbstractPmdReport.getPmdVersion()));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-report-not-render-rule-priority-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfigurationNotRenderRuleViolationPriority(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        String str = readFile(generatedReport);

        // check that there's no priority column
        assertFalse(str.contains("<th>Priority</th>"));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-report-no-render-violations-by-priority.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfigurationNoRenderViolationsByPriority(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        String str = readFile(generatedReport);

        // there should be no section Violations By Priority
        assertFalse(str.contains("Violations By Priority</h2>"));
        assertFalse(str.contains("Priority 3</h3>"));
        assertFalse(str.contains("Priority 4</h3>"));
        // the file App.java is mentioned once: in the files section
        assertEquals(1, StringUtils.countMatches(str, "def/configuration/App.java"));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-with-analysis-cache-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfigurationWithAnalysisCache(PmdReport mojo) throws Exception {
        mojo.execute();

        // check if the PMD analysis cache file has been generated
        File cacheFile =
                new File(getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/pmd/pmd.cache");
        assertTrue(cacheFile.exists());
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "javascript-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testJavascriptConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // these are the rulesets, that have been applied...
        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-bestpractices.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-codestyle.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-errorprone.xml");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedReport);
        assertTrue(str.contains("Avoid using global variables"));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testFileURL(PmdReport mojo) throws Exception {

        // Additional test case for MPMD-174 (https://issues.apache.org/jira/browse/MPMD-174).
        int port = determineFreePort();
        WireMockServer mockServer = new WireMockServer(port);
        mockServer.start();

        String sonarRuleset = IOUtils.toString(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("unit/default-configuration/rulesets/sonar-way-ruleset.xml"),
                StandardCharsets.UTF_8);

        String sonarMainPageHtml = IOUtils.toString(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("unit/default-configuration/rulesets/sonar-main-page.html"),
                StandardCharsets.UTF_8);

        final String sonarBaseUrl = "/profiles";
        final String sonarProfileUrl = sonarBaseUrl + "/export?format=pmd&language=java&name=Sonar%2520way";
        final String sonarExportRulesetUrl = "http://localhost:" + mockServer.port() + sonarProfileUrl;

        WireMock.configureFor("localhost", port);
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(sonarBaseUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(sonarMainPageHtml)));

        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(sonarProfileUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sonarRuleset)));

        URL url = getClass().getClassLoader().getResource("category/java/bestpractices.xml");
        URL url2 = getClass().getClassLoader().getResource("category/java/codestyle.xml");
        URL url3 = getClass().getClassLoader().getResource("category/java/errorprone.xml");
        mojo.setRulesets(new String[] {url.toString(), url2.toString(), url3.toString(), sonarExportRulesetUrl});

        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // the resolved and extracted rulesets
        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-bestpractices.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-codestyle.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-errorprone.xml");
        assertTrue(generatedFile.exists());

        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/004-export_format_pmd_language_java_name_Sonar_2520way.xml");
        assertTrue(generatedFile.exists());

        // check if there's a link to the JXR files
        String str = readFile(generatedReport);

        assertTrue(str.contains("/xref/def/configuration/App.html#L31"));

        assertTrue(str.contains("/xref/def/configuration/AppSample.html#L45"));

        mockServer.stop();
    }

    private int determineFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * With custom rulesets
     *
     * @throws Exception
     */
    @Basedir("/unit/custom-configuration")
    @InjectMojo(goal = "pmd", pom = "custom-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testCustomConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check the generated files
        File generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/pmd.csv");
        assertTrue(generatedFile.exists());

        // 001-maven-pmd-plugin-default.xml is also generated, so we get 002-custom.xml
        generatedFile =
                new File(getBasedir(), "target/test/unit/custom-configuration/target/pmd/rulesets/002-custom.xml");
        assertTrue(generatedFile.exists());

        // check if custom ruleset was applied
        String str = readFile(generatedReport);

        // codestyle.xml/ControlStatementBraces:
        assertTrue(lowerCaseContains(str, "This statement should have braces"));

        // Must be false as codestyle.xml/ControlStatementBraces with checkIfElseStmt=false is used
        assertFalse(lowerCaseContains(str, "Avoid using if...else statements without curly braces"));

        assertFalse(
                lowerCaseContains(str, "Avoid unnecessary constructors - the compiler will generate these for you"),
                "unnecessary constructor should not be triggered because of low priority");

        // veryLongVariableNameWithViolation is really too long
        assertTrue(lowerCaseContains(str, "veryLongVariableNameWithViolation"));
        // notSoLongVariableName should not be reported
        assertFalse(lowerCaseContains(str, "notSoLongVariableName"));
    }

    /**
     * Verify skip parameter
     *
     * @throws Exception
     */
    @Basedir("/unit/custom-configuration")
    @InjectMojo(goal = "pmd", pom = "skip-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSkipConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertFalse(generatedReport.exists());

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File(getBasedir(), "target/test/unit/skip-configuration/target/pmd.csv");
        assertFalse(generatedFile.exists());

        generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/custom.xml");
        assertFalse(generatedFile.exists());

        // the fact, the PMD execution has been skipped, should be logged
        String output = CapturingPrintStream.getOutput();
        assertTrue(output.contains("Skipping org.apache.maven.plugins:maven-pmd-plugin"));
    }

    @Basedir("/unit/empty-report")
    @InjectMojo(goal = "pmd", pom = "skip-empty-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSkipEmptyReportConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertFalse(generatedReport.exists());
    }

    @Basedir("/unit/empty-report")
    @InjectMojo(goal = "pmd", pom = "empty-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testEmptyReportConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // verify the generated files do exist, even if there are no violations
        String str = readFile(generatedReport);
        assertFalse(lowerCaseContains(str, "Hello.java"));
        assertEquals(1, StringUtils.countMatches(str, "PMD found no problems in your source code."));
        // no sections files or violations by priority
        assertFalse(str.contains("Files</h2>"));
        assertFalse(str.contains("Violations By Priority</h2>"));
    }

    @Basedir("/unit/invalid-format")
    @InjectMojo(goal = "pmd", pom = "invalid-format-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testInvalidFormat(PmdReport mojo) {
        try {

            mojo.execute();

            fail("Must nested MavenReportException.");
        } catch (MojoExecutionException e) {
            assertTrue(e.getCause() instanceof MavenReportException);
        }
    }

    @Basedir("/unit/invalid-format")
    @InjectMojo(goal = "pmd", pom = "invalid-target-jdk-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testInvalidTargetJdk(PmdReport mojo) throws Exception {
        try {
            mojo.execute();

            fail("Must nested MavenReportException.");
        } catch (MojoExecutionException e) {
            assertTrue(e.getCause() instanceof MavenReportException);
        }
    }

    /**
     * Verify the pmd.xml file is included in the reports when requested.
     */
    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-report-include-xml-in-reports-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testIncludeXmlInReports(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // verify the pmd file is included in site
        File generatedXmlFile = new File(getBasedir(), "target/test/unit/default-configuration/target/site/pmd.xml");
        assertTrue(generatedXmlFile.exists());

        String pmdXmlTarget = readFile(new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml"));
        assertTrue(pmdXmlTarget.contains("</pmd>"));

        // check that pmd.xml file has the closing element
        String pmdXml = readFile(generatedXmlFile);
        assertTrue(pmdXml.contains("</pmd>"));
    }

    /**
     * Verify the correct working of the locationTemp method.
     */
    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testLocationTemp(PmdReport mojo) {

        assertEquals(
                "001-export_format_pmd_language_java_name_some_2520name.xml",
                mojo.getLocationTemp(
                        "http://nemo.sonarsource.org/sonar/profiles/export?format=pmd&language=java&name=some%2520name",
                        1),
                "locationTemp is not correctly encoding filename");
    }

    /**
     * Verify that suppressMarker works.
     */
    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-with-suppressMarker-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSuppressMarkerConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedFile);

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse(str.contains("Avoid unused private fields such as 'unusedVar2'.\n </violation>"));
        // but it appears as suppressed
        assertTrue(
                str.contains("suppressiontype=\"//nopmd\" msg=\"Avoid unused private fields such as 'unusedVar2'.\""));

        // check if there's a link to the JXR files
        String report = readFile(generatedReport);
        assertTrue(report.contains("/xref/def/configuration/AppSample.html#L27"));
        // suppressed violation
        assertTrue(report.contains("Avoid unused private fields such as 'unusedVar2'."));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-with-suppressMarker-no-render-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSuppressMarkerConfigurationWithoutRendering(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedFile);

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse(str.contains("Avoid unused private fields such as 'unusedVar2'.\n </violation>"));
        // but it appears as suppressed
        assertTrue(
                str.contains("suppressiontype=\"//nopmd\" msg=\"Avoid unused private fields such as 'unusedVar2'.\""));

        // check if there's a link to the JXR files
        String report = readFile(generatedReport);
        assertTrue(report.contains("/xref/def/configuration/AppSample.html#L27"));
        // suppressed violations are not rendered
        assertFalse(report.contains("Avoid unused private fields such as 'unusedVar2'."));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "jsp-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testJspConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // these are the rulesets, that have been applied...
        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-bestpractices.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-codestyle.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-design.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/004-errorprone.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/005-security.xml");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedReport);
        assertTrue(str.contains("JSP file should use UTF-8 encoding"));
        assertTrue(str.contains("Using unsanitized JSP expression can lead to Cross Site Scripting (XSS) attacks"));
        assertTrue(str.contains("Avoid having style information in JSP files."));
    }

    @Basedir("/unit/processing-error")
    @InjectMojo(goal = "pmd", pom = "pmd-processing-error-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testPMDProcessingError(PmdReport mojo) {
        try {
            mojo.execute();

            fail("Expected exception");
        } catch (MojoExecutionException e) {
            assertTrue(e.getCause().getMessage().endsWith("Found 1 PMD processing error"));
        }
    }

    @Basedir("/unit/processing-error")
    @InjectMojo(goal = "pmd", pom = "pmd-processing-error-skip-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testPMDProcessingErrorWithDetailsSkipped(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        String output = CapturingPrintStream.getOutput();
        assertTrue(output.contains("There is 1 PMD processing error:"), output);

        File generatedFile = new File(getBasedir(), "target/test/unit/parse-error/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // The parse exception must be in the XML report
        String xml = readFile(generatedFile);
        assertTrue(xml.contains("ParseException:"));
        assertTrue(xml.contains("at line 23, column 5: Encountered"));

        // The parse exception must also be in the HTML report
        String html = readFile(generatedReport);
        assertTrue(html.contains("ParseException:"));
        assertTrue(html.contains("at line 23, column 5: Encountered"));
    }

    @Basedir("/unit/processing-error")
    @InjectMojo(goal = "pmd", pom = "pmd-processing-error-no-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testPMDProcessingErrorWithDetailsNoReport(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        String output = CapturingPrintStream.getOutput();
        assertTrue(output.contains("There is 1 PMD processing error:"), output);

        File generatedFile = new File(getBasedir(), "target/test/unit/parse-error/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // The parse exception must be in the XML report
        String xml = readFile(generatedFile);
        assertTrue(xml.contains("ParseException:"));
        assertTrue(xml.contains("at line 23, column 5: Encountered"));

        // The parse exception must NOT be in the HTML report, since reportProcessingErrors is false
        String html = readFile(generatedReport);
        assertFalse(html.contains("ParseException:"));
        assertFalse(html.contains("at line 23, column 5: Encountered"));
    }

    @Basedir("/unit/exclude-roots")
    @InjectMojo(goal = "pmd", pom = "pmd-exclude-roots-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testPMDExcludeRootsShouldExcludeSubdirectories(PmdReport mojo) throws Exception {
        mojo.execute();

        File generatedFile = new File(getBasedir(), "target/test/unit/exclude-roots/target/pmd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);

        assertTrue(str.contains("ForLoopShouldBeWhileLoop"), "Seems like all directories are excluded now");
        assertFalse(
                str.contains("OverrideBothEqualsAndHashcode"), "Exclusion of an exact source directory not working");
        assertFalse(
                str.contains("JumbledIncrementer"),
                "Exclusion of base directory with subdirectories not working (MPMD-178)");
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-report-pmd-exclusions-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testViolationExclusion(PmdReport mojo) throws Exception {
        mojo.execute();

        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(generatedFile.exists());
        String str = readFile(generatedFile);

        assertFalse(str.contains("<violation"));
    }

    @Test
    public void testCustomRenderer() throws MavenReportException {
        final Renderer renderer = PmdExecutor.createRenderer("net.sourceforge.pmd.renderers.TextRenderer", "UTF-8");
        assertNotNull(renderer);
    }

    @Test
    public void testCodeClimateRenderer() throws MavenReportException {
        final Renderer renderer =
                PmdExecutor.createRenderer("net.sourceforge.pmd.renderers.CodeClimateRenderer", "UTF-8");
        assertNotNull(renderer);
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-report-custom-rules.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testPmdReportCustomRulesNoExternalInfoUrl(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        String str = readFile(generatedReport);

        // custom rule without link
        assertEquals(2, StringUtils.countMatches(str, "<td>CustomRule</td>"));
        // standard rule with link
        assertEquals(4, StringUtils.countMatches(str, "\">UnusedPrivateField</a></td>"));
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "pmd", pom = "pmd-report-resolve-rulesets.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testPmdReportResolveRulesets(PmdReport mojo) throws Exception {
        int port = determineFreePort();
        WireMockServer mockServer = new WireMockServer(port);
        mockServer.start();

        String sonarRuleset = IOUtils.toString(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("unit/default-configuration/rulesets/sonar-way-ruleset.xml"),
                StandardCharsets.UTF_8);

        final String sonarProfileUrl = "/profiles/export?format=pmd&language=java&name=Sonar%2520way";
        final String sonarExportRulesetUrl = "http://localhost:" + mockServer.port() + sonarProfileUrl;
        final String myRulesetBaseUrl = "/config/my-ruleset.xml";
        final String myRulesetUrl = "http://localhost:" + mockServer.port() + myRulesetBaseUrl;
        final String notAInternalRulesetBaseUrl =
                "/projects/OURPROJECT/repos/ourproject-pmd/raw/InProgressRuleset.xml?at=refs%2Fheads%2Fmaster";
        final String notAInternalRulesetUrl = "http://localhost:" + mockServer.port() + notAInternalRulesetBaseUrl;

        WireMock.configureFor("localhost", port);
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(sonarProfileUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sonarRuleset)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(myRulesetBaseUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sonarRuleset)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(notAInternalRulesetBaseUrl))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(sonarRuleset)));

        mojo.rulesets[3] = sonarExportRulesetUrl;
        mojo.rulesets[4] = myRulesetUrl;
        mojo.rulesets[5] = notAInternalRulesetUrl;
        mojo.execute();

        // these are the rulesets, that have been copied to target/pmd/rulesets
        File generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-custom-rules.xml");
        assertTrue(generatedFile.exists());

        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-bestpractices.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-design.xml");
        assertTrue(generatedFile.exists());

        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/004-export_format_pmd_language_java_name_Sonar_2520way.xml");
        assertTrue(generatedFile.exists());

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/005-my-ruleset.xml");
        assertTrue(generatedFile.exists());

        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/006-InProgressRuleset.xml_at_refs_2Fheads_2Fmaster.xml");
        assertTrue(generatedFile.exists());

        mockServer.stop();
    }

    /**
     * Read the contents of the specified file into a string.
     */
    protected String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
