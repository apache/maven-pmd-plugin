package org.apache.maven.plugins.pmd;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdReportTest
    extends AbstractPmdReportTest
{
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        Locale.setDefault( Locale.ENGLISH );
        FileUtils.deleteDirectory( new File( getBasedir(), "target/test/unit" ) );
    }

    public void testDefaultConfiguration()
        throws Exception
    {
        FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                    "src/test/resources/unit/default-configuration/jxr-files" ),
                                          new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if the rulesets, that have been applied, have been copied
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/maven-pmd-plugin-default.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if there's a link to the JXR files
        String str = readFile( generatedFile );

        assertTrue( str.contains( "/xref/def/configuration/App.html#L31" ) );

        assertTrue( str.contains( "/xref/def/configuration/AppSample.html#L45" ) );

        // check if there's a priority column
        assertTrue( str.contains( "<th>Priority</th>" ) );

        // there should be a rule column
        assertTrue( str.contains( "<th>Rule</th>" ) );
        // along with a link to the rule
        assertTrue( str.contains( "pmd_rules_java_bestpractices.html#unusedprivatefield\">UnusedPrivateField</a>" ) );

        // there should be the section Violations By Priority
        assertTrue( str.contains( "Violations By Priority</h2>" ) );
        assertTrue( str.contains( "Priority 3</h3>" ) );
        assertTrue( str.contains( "Priority 4</h3>" ) );
        // the file App.java is mentioned 3 times: in prio 3, in prio 4 and in the files section
        assertEquals( 3, StringUtils.countMatches( str, "def/configuration/App.java" ) );
    }

    public void testDefaultConfigurationNotRenderRuleViolationPriority()
            throws Exception
    {
        FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                    "src/test/resources/unit/default-configuration/jxr-files" ),
                                          new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-report-not-render-rule-priority-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );

        // check that there's no priority column
        assertFalse( str.contains( "<th>Priority</th>" ) );
    }

    public void testDefaultConfigurationNoRenderViolationsByPriority()
            throws Exception
        {
            FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                        "src/test/resources/unit/default-configuration/jxr-files" ),
                                              new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

            File testPom =
                new File( getBasedir(),
                          "src/test/resources/unit/default-configuration/pmd-report-no-render-violations-by-priority.xml" );
            PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
            mojo.execute();

            File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
            renderer( mojo, generatedFile );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

            String str = readFile( generatedFile );

            // there should be no section Violations By Priority
            assertFalse( str.contains( "Violations By Priority</h2>" ) );
            assertFalse( str.contains( "Priority 3</h3>" ) );
            assertFalse( str.contains( "Priority 4</h3>" ) );
            // the file App.java is mentioned once: in the files section
            assertEquals( 1, StringUtils.countMatches( str, "def/configuration/App.java" ) );
        }


    public void testDefaultConfigurationWithAnalysisCache()
            throws Exception
    {
        FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                    "src/test/resources/unit/default-configuration/jxr-files" ),
                                          new File( getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/site" ) );

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-with-analysis-cache-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD analysis cache file has been generated
        File cacheFile = new File( getBasedir(), "target/test/unit/pmd-with-analysis-cache-plugin-config/target/pmd/pmd.cache" );
        assertTrue( FileUtils.fileExists( cacheFile.getAbsolutePath() ) );
    }

    public void testJavascriptConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/javascript-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // these are the rulesets, that have been applied...
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/bestpractices.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/codestyle.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/errorprone.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );
        assertTrue( str.contains( "Avoid using global variables" ) );
    }

    public void testFileURL()
        throws Exception
    {
        FileUtils.copyDirectoryStructure( new File( getBasedir(),
                                                    "src/test/resources/unit/default-configuration/jxr-files" ),
                                          new File( getBasedir(), "target/test/unit/default-configuration/target/site" ) );

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        // Additional test case for MPMD-174 (https://issues.apache.org/jira/browse/MPMD-174).
        int port = determineFreePort();
        WireMockServer mockServer = new WireMockServer( port );
        mockServer.start();

        String sonarRuleset =
            IOUtils.toString( getClass().getClassLoader().getResourceAsStream( "unit/default-configuration/rulesets/sonar-way-ruleset.xml" ),
                    StandardCharsets.UTF_8 );

        String sonarMainPageHtml =
            IOUtils.toString( getClass().getClassLoader().getResourceAsStream( "unit/default-configuration/rulesets/sonar-main-page.html" ),
                    StandardCharsets.UTF_8 );

        final String sonarBaseUrl = "/profiles";
        final String sonarProfileUrl = sonarBaseUrl + "/export?format=pmd&language=java&name=Sonar%2520way";
        final String sonarExportRulesetUrl = "http://localhost:" + mockServer.port() + sonarProfileUrl;

        mockServer.stubFor( WireMock.get( WireMock.urlEqualTo( sonarBaseUrl ) ).willReturn( WireMock.aResponse().withStatus( 200 ).withHeader( "Content-Type",
                                                                                                                                               "text/html" ).withBody( sonarMainPageHtml ) ) );

        mockServer.stubFor( WireMock.get( WireMock.urlEqualTo( sonarProfileUrl ) ).willReturn( WireMock.aResponse().withStatus( 200 ).withHeader( "Content-Type",
                                                                                                                                                  "text/xml" ).withBody( sonarRuleset ) ) );

        URL url = getClass().getClassLoader().getResource( "rulesets/java/basic.xml" );
        URL url2 = getClass().getClassLoader().getResource( "rulesets/java/unusedcode.xml" );
        URL url3 = getClass().getClassLoader().getResource( "rulesets/java/imports.xml" );
        mojo.setRulesets( new String[] { url.toString(), url2.toString(), url3.toString(), sonarExportRulesetUrl } );

        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // the resolved and extracted rulesets
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/basic.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/imports.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/unusedcode.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile =
            new File( getBasedir(),
                      "target/test/unit/default-configuration/target/pmd/rulesets/export_format_pmd_language_java_name_Sonar_2520way.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if there's a link to the JXR files
        String str = readFile( generatedFile );

        assertTrue( str.contains( "/xref/def/configuration/App.html#L31" ) );

        assertTrue( str.contains( "/xref/def/configuration/AppSample.html#L45" ) );

        mockServer.stop();
    }

    private int determineFreePort()
    {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException( "Couldn't find a free port.", e );
        }
    }

    /**
     * With custom rulesets
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/custom-configuration/custom-configuration-plugin-config.xml" );

        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check the generated files
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/pmd.csv" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/pmd/rulesets/custom.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check if custom ruleset was applied
        String str = readFile( generatedFile );

        // codestyle.xml/ControlStatementBraces:
        assertTrue( lowerCaseContains( str, "This statement should have braces" ) );

        // Must be false as codestyle.xml/ControlStatementBraces with checkIfElseStmt=false is used
        assertFalse( lowerCaseContains( str, "Avoid using if...else statements without curly braces" ) );

        assertFalse( "unnecessary constructor should not be triggered because of low priority",
                    lowerCaseContains( str, "Avoid unnecessary constructors - the compiler will generate these for you" ) );

        // veryLongVariableNameWithViolation is really too long
        assertTrue( lowerCaseContains( str, "veryLongVariableNameWithViolation" ) );
        // notSoLongVariableName should not be reported
        assertFalse( lowerCaseContains( str, "notSoLongVariableName" ) );
    }

    /**
     * Verify skip parameter
     *
     * @throws Exception
     */
    public void testSkipConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/custom-configuration/skip-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File( getBasedir(), "target/test/unit/skip-configuration/target/pmd.csv" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/custom.xml" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/pmd.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testSkipEmptyReportConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(), "src/test/resources/unit/empty-report/skip-empty-report-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // verify the generated files do not exist because PMD was skipped
        File generatedFile = new File( getBasedir(), "target/test/unit/empty-report/target/site/pmd.html" );
        assertFalse( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
    }

    public void testEmptyReportConfiguration()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/empty-report/empty-report-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // verify the generated files do exist, even if there are no violations
        File generatedFile = new File( getBasedir(), "target/test/unit/empty-report/target/site/pmd.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( generatedFile );
        assertFalse( lowerCaseContains( str, "Hello.java" ) );
        assertEquals( 1, StringUtils.countMatches( str, "PMD found no problems in your source code." ) );
        // no sections files or violations by priority
        assertFalse( str.contains( "Files</h2>" ) );
        assertFalse( str.contains( "Violations By Priority</h2>" ) );
    }

    public void testInvalidFormat()
        throws Exception
    {
        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/invalid-format/invalid-format-plugin-config.xml" );
            PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
            setVariableValueToObject( mojo, "compileSourceRoots", mojo.project.getCompileSourceRoots() );
            mojo.executeReport( Locale.ENGLISH );

            fail( "Must throw MavenReportException." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    public void testInvalidTargetJdk()
        throws Exception
    {
        try
        {
            File testPom =
                new File( getBasedir(), "src/test/resources/unit/invalid-format/invalid-target-jdk-plugin-config.xml" );
            PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
            mojo.execute();

            fail( "Must throw MavenReportException." );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    /**
     * verify the pmd.xml file is included in the site when requested.
     * @throws Exception
     */
    public void testIncludeXmlInSite()
            throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/default-configuration/pmd-report-include-xml-in-site-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        // verify the pmd file is included in site
        File generatedXmlFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedXmlFile.getAbsolutePath() ) );

        String pmdXmlTarget = readFile( new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" ) );
        assertTrue( pmdXmlTarget.contains( "</pmd>" ) );

        // check that pmd.xml file has the closing element
        String pmdXml = readFile( generatedXmlFile );
        assertTrue( pmdXml.contains( "</pmd>" ) );
    }

    /**
     * Read the contents of the specified file object into a string
     *
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws java.io.IOException
     */
    private String readFile( File file )
        throws IOException
    {
        try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) )
        {
            final StringBuilder str = new StringBuilder( (int) file.length() );

            for ( String line = reader.readLine(); line != null; line = reader.readLine() )
            {
                str.append( ' ' );
                str.append( line );
                str.append( '\n' );
            }
            return str.toString();
        }
    }

    /**
     * Verify the correct working of the locationTemp method
     *
     * @throws Exception
     */
    public void testLocationTemp()
        throws Exception
    {

        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        assertEquals( "locationTemp is not correctly encoding filename",
                      "export_format_pmd_language_java_name_some_2520name.xml",
                      mojo.getLocationTemp( "http://nemo.sonarsource.org/sonar/profiles/export?format=pmd&language=java&name=some%2520name" ) );

    }

    /**
     * Verify that suppressMarker works
     *
     * @throws Exception
     */
    public void testSuppressMarkerConfiguration()
        throws Exception
    {
        File testPom =
            new File( getBasedir(),
                      "src/test/resources/unit/default-configuration/pmd-with-suppressMarker-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );

        // check that there is no violation reported for "unusedVar2" - as it is suppressed
        assertFalse( str.contains( "Avoid unused private fields such as 'unusedVar2'." ) );
    }

    public void testJspConfiguration()
            throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/unit/default-configuration/jsp-configuration-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        mojo.execute();

        // check if the PMD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // these are the rulesets, that have been applied...
        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/bestpractices.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/codestyle.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/design.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/errorprone.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd/rulesets/security.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
        renderer( mojo, generatedFile );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedFile );
        assertTrue(str.contains("JSP file should use UTF-8 encoding"));
        assertTrue(str.contains("Using unsanitized JSP expression can lead to Cross Site Scripting (XSS) attacks"));
        assertTrue(str.contains("Avoid having style information in JSP files."));
    }

    public void testPMDProcessingError()
            throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/unit/processing-error/pmd-processing-error-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
        try {
            mojo.execute();
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertTrue( e.getMessage().endsWith( "Found 1 PMD processing errors" ) );
        }
    }

    public void testPMDProcessingErrorWithDetailsSkipped()
            throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/unit/processing-error/pmd-processing-error-skip-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        PrintStream originalOut = System.out;
        ByteArrayOutputStream logging = new ByteArrayOutputStream();
        System.setOut( new PrintStream( logging ) );

        try {
            mojo.execute();
            String output = logging.toString();
            assertTrue ( output.contains( "There are 1 PMD processing errors:" ) );

            File generatedFile = new File( getBasedir(), "target/test/unit/parse-error/target/pmd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );
            assertTrue( str.contains( "Error while parsing" ) );
            // The parse exception must be in the XML report
            assertTrue( str.contains( "ParseException: Encountered \"\" at line 23, column 5." ) );

            generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
            renderer( mojo, generatedFile );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            str = readFile( generatedFile );
            // The parse exception must also be in the HTML report
            assertTrue( str.contains( "ParseException: Encountered \"\" at line 23, column 5." ) );

        } finally {
            System.setOut( originalOut );
            System.out.println( logging.toString() );
        }
    }

    public void testPMDProcessingErrorWithDetailsNoReport()
            throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/unit/processing-error/pmd-processing-error-no-report-plugin-config.xml" );
        PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );

        PrintStream originalOut = System.out;
        ByteArrayOutputStream logging = new ByteArrayOutputStream();
        System.setOut( new PrintStream( logging ) );

        try {
            mojo.execute();
            String output = logging.toString();
            assertTrue ( output.contains( "There are 1 PMD processing errors:" ) );

            File generatedFile = new File( getBasedir(), "target/test/unit/parse-error/target/pmd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );
            assertTrue( str.contains( "Error while parsing" ) );
            // The parse exception must be in the XML report
            assertTrue( str.contains( "ParseException: Encountered \"\" at line 23, column 5." ) );

            generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/site/pmd.html" );
            renderer( mojo, generatedFile );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            str = readFile( generatedFile );
            // The parse exception must NOT be in the HTML report, since reportProcessingErrors is false
            assertFalse( str.contains( "ParseException: Encountered \"\" at line 23, column 5." ) );

        } finally {
            System.setOut( originalOut );
            System.out.println( logging.toString() );
        }
    }

    public void testPMDExcludeRootsShouldExcludeSubdirectories() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/exclude-roots/pmd-exclude-roots-plugin-config.xml");
        PmdReport mojo = (PmdReport) lookupMojo ("pmd", testPom);
        mojo.execute();

        File generatedFile = new File( getBasedir(), "target/test/unit/exclude-roots/target/pmd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( generatedFile );

        assertTrue( "Seems like all directories are excluded now", str.contains("ForLoopShouldBeWhileLoop") );
        assertFalse( "Exclusion of an exact source directory not working", str.contains( "OverrideBothEqualsAndHashcode" ) );
        assertFalse( "Exclusion of basedirectory with subdirectories not working (MPMD-178)", str.contains( "JumbledIncrementer") );
    }

    public void testViolationExclusion()
            throws Exception
        {
            File testPom =
                new File( getBasedir(),
                          "src/test/resources/unit/default-configuration/pmd-report-pmd-exclusions-configuration-plugin-config.xml" );
            final PmdReport mojo = (PmdReport) lookupMojo( "pmd", testPom );
            mojo.execute();

            File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/pmd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );

            assertEquals(0, StringUtils.countMatches(str, "<violation"));
        }
}
