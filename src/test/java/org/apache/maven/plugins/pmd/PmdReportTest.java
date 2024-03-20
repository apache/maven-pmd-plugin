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
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.sourceforge.pmd.renderers.Renderer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.exec.PmdExecutor;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdReportTest extends AbstractPmdReportTestCase {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.deleteDirectory(new File(getBasedir(), "target/test/unit"));
    }

    public void testDefaultConfiguration() throws Exception {
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File generatedReport = generateReport("pmd", "default-configuration/default-configuration-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // check if the rulesets, that have been applied, have been copied
        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/001-maven-pmd-plugin-default.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

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

    public void testDefaultConfigurationNotRenderRuleViolationPriority() throws Exception {
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File generatedReport = generateReport(
                getGoal(), "default-configuration/pmd-report-not-render-rule-priority-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        String str = readFile(generatedReport);

        // check that there's no priority column
        assertFalse(str.contains("<th>Priority</th>"));
    }

    public void testDefaultConfigurationNoRenderViolationsByPriority() throws Exception {
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File generatedReport =
                generateReport(getGoal(), "default-configuration/pmd-report-no-render-violations-by-priority.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        String str = readFile(generatedReport);

        // there should be no section Violations By Priority
        assertFalse(str.contains("Violations By Priority</h2>"));
        assertFalse(str.contains("Priority 3</h3>"));
        assertFalse(str.contains("Priority 4</h3>"));
        // the file App.java is mentioned once: in the files section
        assertEquals(1, StringUtils.countMatches(str, "def/configuration/App.java"));
    }

    public void testDefaultConfigurationWithAnalysisCache() throws Exception {
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/site"));

        generateReport(getGoal(), "default-configuration/pmd-with-analysis-cache-plugin-config.xml");

        // check if the PMD analysis cache file has been generated
        File cacheFile =
                new File(getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/pmd/pmd.cache");
        assertTrue(FileUtils.fileExists(cacheFile.getAbsolutePath()));
    }

    public void testJavascriptConfiguration() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "default-configuration/javascript-configuration-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // these are the rulesets, that have been applied...
        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-bestpractices.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-codestyle.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-errorprone.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        String str = readFile(generatedReport);
        assertTrue(str.contains("Avoid using global variables"));
    }

    public void testFileURL() throws Exception {
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File testPom = new File(
                getBasedir(), "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml");
        PmdReport mojo = (PmdReport) createReportMojo(getGoal(), testPom);

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

        URL url = getClass().getClassLoader().getResource("rulesets/java/basic.xml");
        URL url2 = getClass().getClassLoader().getResource("rulesets/java/unusedcode.xml");
        URL url3 = getClass().getClassLoader().getResource("rulesets/java/imports.xml");
        mojo.setRulesets(new String[] {url.toString(), url2.toString(), url3.toString(), sonarExportRulesetUrl});

        File generatedReport = generateReport(mojo, testPom);
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // the resolved and extracted rulesets
        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-basic.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-unusedcode.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-imports.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/004-export_format_pmd_language_java_name_Sonar_2520way.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // check if there's a link to the JXR files
        String str = readFile(generatedReport);

        assertTrue(str.contains("/xref/def/configuration/App.html#L31"));

        assertTrue(str.contains("/xref/def/configuration/AppSample.html#L45"));

        mockServer.stop();
    }

    private int determineFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't find a free port.", e);
        }
    }

    /**
     * With custom rulesets
     *
     * @throws Exception
     */
    public void testCustomConfiguration() throws Exception {
        File generatedReport = generateReport(getGoal(), "custom-configuration/custom-configuration-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check the generated files
        File generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/pmd.csv");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // 001-maven-pmd-plugin-default.xml is also generated, so we get 002-custom.xml
        generatedFile =
                new File(getBasedir(), "target/test/unit/custom-configuration/target/pmd/rulesets/002-custom.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // check if custom ruleset was applied
        String str = readFile(generatedReport);

        // codestyle.xml/ControlStatementBraces:
        assertTrue(lowerCaseContains(str, "This statement should have braces"));

        // Must be false as codestyle.xml/ControlStatementBraces with checkIfElseStmt=false is used
        assertFalse(lowerCaseContains(str, "Avoid using if...else statements without curly braces"));

        assertFalse(
                "unnecessary constructor should not be triggered because of low priority",
                lowerCaseContains(str, "Avoid unnecessary constructors - the compiler will generate these for you"));

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
    public void testSkipConfiguration() throws Exception {
        File generatedReport = generateReport(getGoal(), "custom-configuration/skip-plugin-config.xml");
        assertFalse(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File(getBasedir(), "target/test/unit/skip-configuration/target/pmd.csv");
        assertFalse(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(getBasedir(), "target/test/unit/custom-configuration/target/custom.xml");
        assertFalse(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // the fact, the PMD execution has been skipped, should be logged
        String output = CapturingPrintStream.getOutput();
        assertTrue(output.contains("Skipping org.apache.maven.plugins:maven-pmd-plugin"));
    }

    public void testSkipEmptyReportConfiguration() throws Exception {
        // verify the generated files do not exist because PMD was skipped
        File generatedReport = generateReport(getGoal(), "empty-report/skip-empty-report-plugin-config.xml");
        assertFalse(FileUtils.fileExists(generatedReport.getAbsolutePath()));
    }

    public void testEmptyReportConfiguration() throws Exception {
        File generatedReport = generateReport(getGoal(), "empty-report/empty-report-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // verify the generated files do exist, even if there are no violations
        String str = readFile(generatedReport);
        assertFalse(lowerCaseContains(str, "Hello.java"));
        assertEquals(1, StringUtils.countMatches(str, "PMD found no problems in your source code."));
        // no sections files or violations by priority
        assertFalse(str.contains("Files</h2>"));
        assertFalse(str.contains("Violations By Priority</h2>"));
    }

    public void testInvalidFormat() throws Exception {
        try {
            File testPom =
                    new File(getBasedir(), "src/test/resources/unit/invalid-format/invalid-format-plugin-config.xml");
            AbstractPmdReport mojo = createReportMojo(getGoal(), testPom);
            setVariableValueToObject(
                    mojo, "compileSourceRoots", mojo.getProject().getCompileSourceRoots());
            generateReport(mojo, testPom);

            fail("Must throw MavenReportException.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    public void testInvalidTargetJdk() throws Exception {
        try {
            generateReport(getGoal(), "empty-report/invalid-format/invalid-target-jdk-plugin-config.xml");

            fail("Must throw MavenReportException.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * verify the pmd.xml file is included in the reports when requested.
     * @throws Exception
     */
    public void testIncludeXmlInReports() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "default-configuration/pmd-report-include-xml-in-reports-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // verify the pmd file is included in site
        File generatedXmlFile = new File(getBasedir(), "target/test/unit/default-configuration/target/site/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedXmlFile.getAbsolutePath()));

        String pmdXmlTarget = readFile(new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml"));
        assertTrue(pmdXmlTarget.contains("</pmd>"));

        // check that pmd.xml file has the closing element
        String pmdXml = readFile(generatedXmlFile);
        assertTrue(pmdXml.contains("</pmd>"));
    }

    /**
     * Verify the correct working of the locationTemp method
     *
     * @throws Exception
     */
    public void testLocationTemp() throws Exception {

        File testPom = new File(
                getBasedir(), "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo(getGoal(), testPom);

        assertEquals(
                "locationTemp is not correctly encoding filename",
                "001-export_format_pmd_language_java_name_some_2520name.xml",
                mojo.getLocationTemp(
                        "http://nemo.sonarsource.org/sonar/profiles/export?format=pmd&language=java&name=some%2520name",
                        1));
    }

    /**
     * Verify that suppressMarker works
     *
     * @throws Exception
     */
    public void testSuppressMarkerConfiguration() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "default-configuration/pmd-with-suppressMarker-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        String str = readFile(generatedFile);

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse(str.contains("Avoid unused private fields such as 'unusedVar2'.\n </violation>"));
        // but it appears as suppressed
        assertTrue(str.contains("suppressiontype=\"nopmd\" msg=\"Avoid unused private fields such as 'unusedVar2'.\""));

        // check if there's a link to the JXR files
        str = readFile(generatedReport);

        assertTrue(str.contains("/xref/def/configuration/AppSample.html#L27"));
        // suppressed violation
        assertTrue(str.contains("Avoid unused private fields such as 'unusedVar2'."));
    }

    public void testSuppressMarkerConfigurationWithoutRendering() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "default-configuration/pmd-with-suppressMarker-no-render-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        String str = readFile(generatedFile);

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse(str.contains("Avoid unused private fields such as 'unusedVar2'.\n </violation>"));
        // but it appears as suppressed
        assertTrue(str.contains("suppressiontype=\"nopmd\" msg=\"Avoid unused private fields such as 'unusedVar2'.\""));

        // check if there's a link to the JXR files
        str = readFile(generatedReport);

        assertTrue(str.contains("/xref/def/configuration/AppSample.html#L27"));
        // suppressed violations are not rendered
        assertFalse(str.contains("Avoid unused private fields such as 'unusedVar2'."));
    }

    public void testJspConfiguration() throws Exception {
        File generatedReport = generateReport(getGoal(), "default-configuration/jsp-configuration-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        // these are the rulesets, that have been applied...
        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-bestpractices.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-codestyle.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-design.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/004-errorprone.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/005-security.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        String str = readFile(generatedReport);
        assertTrue(str.contains("JSP file should use UTF-8 encoding"));
        assertTrue(str.contains("Using unsanitized JSP expression can lead to Cross Site Scripting (XSS) attacks"));
        assertTrue(str.contains("Avoid having style information in JSP files."));
    }

    public void testPMDProcessingError() throws Exception {
        try {
            generateReport(getGoal(), "processing-error/pmd-processing-error-plugin-config.xml");
            fail("Expected exception");
        } catch (MojoExecutionException e) {
            assertTrue(e.getCause().getMessage().endsWith("Found 1 PMD processing errors"));
        }
    }

    public void testPMDProcessingErrorWithDetailsSkipped() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "processing-error/pmd-processing-error-skip-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        String output = CapturingPrintStream.getOutput();
        assertTrue(output.contains("There are 1 PMD processing errors:"));

        File generatedFile = new File(getBasedir(), "target/test/unit/parse-error/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
        String str = readFile(generatedFile);
        assertTrue(str.contains("Error while parsing"));
        // The parse exception must be in the XML report
        assertTrue(str.contains("ParseException: Encountered \"\" at line 23, column 5."));

        str = readFile(generatedReport);
        // The parse exception must also be in the HTML report
        assertTrue(str.contains("ParseException: Encountered \"\" at line 23, column 5."));
    }

    public void testPMDProcessingErrorWithDetailsNoReport() throws Exception {
        File generatedReport =
                generateReport(getGoal(), "processing-error/pmd-processing-error-no-report-plugin-config.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        String output = CapturingPrintStream.getOutput();
        assertTrue(output.contains("There are 1 PMD processing errors:"));

        File generatedFile = new File(getBasedir(), "target/test/unit/parse-error/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
        String str = readFile(generatedFile);
        assertTrue(str.contains("Error while parsing"));
        // The parse exception must be in the XML report
        assertTrue(str.contains("ParseException: Encountered \"\" at line 23, column 5."));

        str = readFile(generatedReport);
        // The parse exception must NOT be in the HTML report, since reportProcessingErrors is false
        assertFalse(str.contains("ParseException: Encountered \"\" at line 23, column 5."));
    }

    public void testPMDExcludeRootsShouldExcludeSubdirectories() throws Exception {
        generateReport(getGoal(), "exclude-roots/pmd-exclude-roots-plugin-config.xml");

        File generatedFile = new File(getBasedir(), "target/test/unit/exclude-roots/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
        String str = readFile(generatedFile);

        assertTrue("Seems like all directories are excluded now", str.contains("ForLoopShouldBeWhileLoop"));
        assertFalse(
                "Exclusion of an exact source directory not working", str.contains("OverrideBothEqualsAndHashcode"));
        assertFalse(
                "Exclusion of basedirectory with subdirectories not working (MPMD-178)",
                str.contains("JumbledIncrementer"));
    }

    public void testViolationExclusion() throws Exception {
        generateReport(getGoal(), "default-configuration/pmd-report-pmd-exclusions-configuration-plugin-config.xml");

        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration/target/pmd.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));
        String str = readFile(generatedFile);

        assertEquals(0, StringUtils.countMatches(str, "<violation"));
    }

    public void testCustomRenderer() throws MavenReportException {
        final Renderer renderer = PmdExecutor.createRenderer("net.sourceforge.pmd.renderers.TextRenderer", "UTF-8");
        assertNotNull(renderer);
    }

    public void testCodeClimateRenderer() throws MavenReportException {
        final Renderer renderer =
                PmdExecutor.createRenderer("net.sourceforge.pmd.renderers.CodeClimateRenderer", "UTF-8");
        assertNotNull(renderer);
    }

    public void testPmdReportCustomRulesNoExternalInfoUrl() throws Exception {
        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File generatedReport = generateReport(getGoal(), "default-configuration/pmd-report-custom-rules.xml");
        assertTrue(FileUtils.fileExists(generatedReport.getAbsolutePath()));

        String str = readFile(generatedReport);

        // custom rule without link
        assertEquals(2, StringUtils.countMatches(str, "<td>CustomRule</td>"));
        // standard rule with link
        assertEquals(4, StringUtils.countMatches(str, "\">UnusedPrivateField</a></td>"));
    }

    public void testPmdReportResolveRulesets() throws Exception {
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

        FileUtils.copyDirectoryStructure(
                new File(getBasedir(), "src/test/resources/unit/default-configuration/jxr-files"),
                new File(getBasedir(), "target/test/unit/default-configuration/target/site"));

        File testPom =
                new File(getBasedir(), "src/test/resources/unit/default-configuration/pmd-report-resolve-rulesets.xml");
        PmdReport mojo = (PmdReport) createReportMojo(getGoal(), testPom);
        mojo.rulesets[3] = sonarExportRulesetUrl;
        mojo.rulesets[4] = myRulesetUrl;
        mojo.rulesets[5] = notAInternalRulesetUrl;
        generateReport(mojo, testPom);

        // these are the rulesets, that have been copied to target/pmd/rulesets
        File generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/001-custom-rules.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/002-bestpractices.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/003-java-design.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/004-export_format_pmd_language_java_name_Sonar_2520way.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile =
                new File(getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/005-my-ruleset.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration/target/pmd/rulesets/006-InProgressRuleset.xml_at_refs_2Fheads_2Fmaster.xml");
        assertTrue(FileUtils.fileExists(generatedFile.getAbsolutePath()));

        mockServer.stop();
    }

    @Override
    protected String getGoal() {
        return "pmd";
    }
}
