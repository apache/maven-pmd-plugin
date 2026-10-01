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

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests behavior when there are no files to process.
 */
@MojoTest(realRepositorySession = true)
@Basedir("/unit/default-configuration-no-files")
public class PmdReportNoFilesTest {
    @BeforeEach
    public void setUp() throws Exception {
        CapturingPrintStream.init(true);
    }

    @InjectMojo(goal = "pmd", pom = "default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfigurationNoFiles(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration-no-files/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // check if the rulesets, that have been applied, have been copied
        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration-no-files/target/pmd/rulesets/001-maven-pmd-plugin-default.xml");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedReport);
        assertTrue(str.contains("PMD found no problems in your source code."));

        String output = CapturingPrintStream.getOutput();
        assertFalse(output.contains("[WARNING] No files to analyze.")); // this log message comes from PMD.
        assertTrue(output.contains("No files found to process. Skipping PMD execution."));

        // the version is not logged, since PMD execution is skipped
        assertFalse(output.contains("PMD version: "));
    }

    @InjectMojo(goal = "pmd", pom = "include-xml-in-reports-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testIncludeXmlInReportsNoFiles(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile =
                new File(getBasedir(), "target/test/unit/include-xml-in-reports-configuration-no-files/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // the pmd.xml should have been copied to the site
        File reportXml = new File(outputDir, "pmd.xml");
        assertTrue(reportXml.exists());
    }

    @InjectMojo(goal = "pmd", pom = "skip-empty-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSkipEmptyReportConfiguration(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertFalse(generatedReport.exists());

        // the (empty) pmd.xml file still needs to be created for the check mojo.
        File generatedFile = new File(getBasedir(), "target/test/unit/skip-empty-report-no-files/target/pmd.xml");
        assertTrue(generatedFile.exists());

        String output = CapturingPrintStream.getOutput();
        assertFalse(output.contains("[WARNING] No files to analyze.")); // this log message comes from PMD.
        assertTrue(output.contains("No files found to process. Skipping PMD execution."));

        // the version is not logged, since PMD execution is skipped
        assertFalse(output.contains("PMD version: "));
    }

    @InjectMojo(goal = "pmd", pom = "javascript-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testJavascriptConfigurationNoFilesToProcess(PmdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(generatedReport.exists());

        // check if the PMD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration-no-files/target/pmd.xml");
        assertTrue(generatedFile.exists());

        // these are the rulesets, that have been applied...
        generatedFile = new File(
                getBasedir(),
                "target/test/unit/default-configuration-no-files/target/pmd/rulesets/001-bestpractices.xml");
        assertTrue(generatedFile.exists());

        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration-no-files/target/pmd/rulesets/002-codestyle.xml");
        assertTrue(generatedFile.exists());

        generatedFile = new File(
                getBasedir(), "target/test/unit/default-configuration-no-files/target/pmd/rulesets/003-errorprone.xml");
        assertTrue(generatedFile.exists());

        String str = readFile(generatedReport);
        assertTrue(str.contains("PMD found no problems in your source code."));

        String output = CapturingPrintStream.getOutput();
        assertFalse(output.contains("[WARNING] No files to analyze.")); // this log message comes from PMD.
        assertTrue(output.contains("No files found to process. Skipping PMD execution."));

        // the version is not logged, since PMD execution is skipped
        assertFalse(output.contains("PMD version: "));
    }

    /**
     * Read the contents of the specified file into a string.
     */
    protected String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
