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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class CpdReportTest
    extends AbstractPmdReportTestCase
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

    /**
     * Test CPDReport given the default configuration
     *
     * @throws Exception
     */
    public void testDefaultConfiguration()
        throws Exception
    {
        File generatedReport = generateReport( "cpd", "default-configuration/cpd-default-configuration-plugin-config.xml" );
        assertTrue( FileUtils.fileExists( generatedReport.getAbsolutePath() ) );

        // check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check the contents of cpd.html
        String str = readFile( generatedReport );
        assertTrue( lowerCaseContains( str, "AppSample.java" ) );
        assertTrue( lowerCaseContains( str, "App.java" ) );
        assertTrue( lowerCaseContains( str, "public String dup( String str )" ) );
        assertTrue( lowerCaseContains( str, "tmp = tmp + str.substring( i, i + 1);" ) );

        // the version should be logged
        String output = CapturingPrintStream.getOutput();
        assertTrue ( output.contains( "PMD version: " + AbstractPmdReport.getPmdVersion() ) );
    }

    /**
     * Test CPDReport with the text renderer given as "format=txt"
     *
     * @throws Exception
     */
    public void testTxtFormat()
        throws Exception
    {
        generateReport( "cpd", "custom-configuration/cpd-txt-format-configuration-plugin-config.xml" );

        // check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/cpd.txt" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        // check the contents of cpd.txt
        String str = readFile( generatedFile );
        // Contents that should NOT be in the report
        assertFalse( lowerCaseContains( str, "public static void main( String[] args )" ) );
        // Contents that should be in the report
        assertTrue( lowerCaseContains( str, "public void duplicateMethod( int i )" ) );
    }

    /**
     * Test CPDReport using custom configuration
     *
     * @throws Exception
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File generatedReport = generateReport( "cpd", "custom-configuration/cpd-custom-configuration-plugin-config.xml" );
        assertTrue( FileUtils.fileExists( generatedReport.getAbsolutePath() ) );

        // check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/custom-configuration/target/cpd.csv" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        String str = readFile( generatedReport );
        // Contents that should NOT be in the report
        assertFalse( lowerCaseContains( str, "/Sample.java" ) );
        assertFalse( lowerCaseContains( str, "public void duplicateMethod( int i )" ) );
        // Contents that should be in the report
        assertTrue( lowerCaseContains( str, "AnotherSample.java" ) );
        assertTrue( lowerCaseContains( str, "public static void main( String[] args )" ) );
        assertTrue( lowerCaseContains( str, "private String unusedMethod(" ) );
    }

    /**
     * Test CPDReport with invalid format
     *
     * @throws Exception
     */
    public void testInvalidFormat()
        throws Exception
    {
        try
        {
            File testPom =
                    new File( getBasedir(), "src/test/resources/unit/invalid-format/cpd-invalid-format-plugin-config.xml" );
            AbstractPmdReport mojo  = createReportMojo( "cpd", testPom );
            setVariableValueToObject( mojo, "compileSourceRoots", mojo.getProject().getCompileSourceRoots() );
            generateReport( mojo, testPom );

            fail( "MavenReportException must be thrown" );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

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
        String strTmp;
        StringBuilder str = new StringBuilder( (int) file.length() );
        try ( BufferedReader in = new BufferedReader( new FileReader( file ) ) )
        {
            while ( ( strTmp = in.readLine() ) != null )
            {
                str.append( ' ' );
                str.append( strTmp );
            }
        }

        return str.toString();
    }

    public void testWriteNonHtml()
        throws Exception
    {
        generateReport( "cpd", "default-configuration/cpd-default-configuration-plugin-config.xml" );

        // check if the CPD files were generated
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document pmdCpdDocument = builder.parse( generatedFile );
        assertNotNull( pmdCpdDocument );

        String str = readFile( generatedFile );
        assertTrue( lowerCaseContains( str, "AppSample.java" ) );
        assertTrue( lowerCaseContains( str, "App.java" ) );
        assertTrue( lowerCaseContains( str, "public String dup( String str )" ) );
        assertTrue( lowerCaseContains( str, "tmp = tmp + str.substring( i, i + 1);" ) );
    }

    /**
     * verify the cpd.xml file is included in the site when requested.
     * @throws Exception
     */
    public void testIncludeXmlInSite()
            throws Exception
    {
        generateReport( "cpd", "default-configuration/cpd-report-include-xml-in-site-plugin-config.xml" );

        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document pmdCpdDocument = builder.parse( generatedFile );
        assertNotNull( pmdCpdDocument );

        String str = readFile( generatedFile );
        assertTrue( str.contains( "</pmd-cpd>" ) );

        File siteReport = new File( getBasedir(), "target/test/unit/default-configuration/target/site/cpd.xml" );
        assertTrue( FileUtils.fileExists( siteReport.getAbsolutePath() ) );
        String siteReportContent = readFile( siteReport );
        assertTrue( siteReportContent.contains( "</pmd-cpd>" ) );
        assertEquals( str, siteReportContent );
    }


    public void testSkipEmptyReportConfiguration()
        throws Exception
    {
        // verify the generated files do not exist because PMD was skipped
        File generatedReport = generateReport( "cpd", "empty-report/cpd-skip-empty-report-plugin-config.xml" );
        assertFalse( FileUtils.fileExists( generatedReport.getAbsolutePath() ) );
    }

    public void testEmptyReportConfiguration()
        throws Exception
    {
        // verify the generated files do exist, even if there are no violations
        File generatedReport = generateReport( "cpd", "empty-report/cpd-empty-report-plugin-config.xml" );
        assertTrue( FileUtils.fileExists( generatedReport.getAbsolutePath() ) );

        String str = readFile( generatedReport );
        assertFalse( lowerCaseContains( str, "Hello.java" ) );
        assertTrue( str.contains( "CPD found no problems in your source code." ) );
    }

    public void testCpdEncodingConfiguration()
        throws Exception
    {
        String originalEncoding = System.getProperty( "file.encoding" );
        try
        {
            System.setProperty( "file.encoding", "UTF-16" );

            generateReport( "cpd", "default-configuration/cpd-default-configuration-plugin-config.xml" );

            // check if the CPD files were generated
            File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
            assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
            String str = readFile( generatedFile );
            assertTrue( lowerCaseContains( str, "AppSample.java" ) );
        }
        finally
        {
            System.setProperty( "file.encoding", originalEncoding );
        }
    }

    public void testCpdJavascriptConfiguration()
        throws Exception
    {
        generateReport( "cpd", "default-configuration/cpd-javascript-plugin-config.xml" );

        // verify  the generated file to exist and violations are reported
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( generatedFile );
        assertTrue( lowerCaseContains( str, "Sample.js" ) );
        assertTrue( lowerCaseContains( str, "SampleDup.js" ) );
    }

    public void testCpdJspConfiguration()
            throws Exception
    {
        generateReport( "cpd", "default-configuration/cpd-jsp-plugin-config.xml" );

        // verify  the generated file to exist and violations are reported
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( generatedFile );
        assertTrue( lowerCaseContains( str, "sample.jsp" ) );
        assertTrue( lowerCaseContains( str, "sampleDup.jsp" ) );
    }

    public void testExclusionsConfiguration()
            throws Exception
    {
        generateReport( "cpd", "default-configuration/cpd-report-cpd-exclusions-configuration-plugin-config.xml" );

        // verify  the generated file to exist and no duplications are reported
        File generatedFile = new File( getBasedir(), "target/test/unit/default-configuration/target/cpd.xml" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );
        String str = readFile( generatedFile );
        assertEquals( 0, StringUtils.countMatches( str, "<duplication" ) );
    }
}
