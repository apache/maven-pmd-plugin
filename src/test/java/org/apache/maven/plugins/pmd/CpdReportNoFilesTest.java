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
import java.nio.file.Files;
import java.util.Collections;

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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.testing.PlexusExtension;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests behavior when there are no files to process.
 */
@MojoTest
@Basedir("/unit/default-configuration-no-files")
public class CpdReportNoFilesTest {

    @Inject
    private MavenSession mavenSession;

    @Inject
    private DefaultRepositorySystemSessionFactory repoSessionFactory;

    @Inject
    private MavenProject testMavenProject;

    @Inject
    private MojoExecution mojoExecution;

    @BeforeEach
    public void setUp() {
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
        Mockito.when(testMavenProject.getRemoteProjectRepositories())
                .thenReturn(Collections.singletonList(centralRepo));

        Mockito.when(mojoExecution.getPlugin()).thenReturn(new Plugin());
    }

    @InjectMojo(goal = "cpd", pom = "cpd-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testDefaultConfigurationNoFiles(CpdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(new File(generatedReport.getAbsolutePath()).exists());

        // check if the CPD files were generated
        File generatedFile = new File(getBasedir(), "target/test/unit/default-configuration-no-files/target/cpd.xml");
        assertTrue(generatedFile.exists());

        // check the contents of cpd.html
        String str = readFile(generatedReport);
        assertTrue(str.contains("CPD found no problems in your source code."));

        String output = CapturingPrintStream.getOutput();
        assertFalse(output.contains("[WARNING] No files to analyze.")); // this log message comes from PMD.
        assertTrue(output.contains("No files found to process. Skipping CPD execution."));
    }

    @InjectMojo(goal = "cpd", pom = "cpd-include-xml-in-reports-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testIncludeXmlInReportsNoFiles(CpdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertTrue(new File(generatedReport.getAbsolutePath()).exists());

        // check if the CPD files were generated
        File generatedFile =
                new File(getBasedir(), "target/test/unit/include-xml-in-reports-configuration-no-files/target/cpd.xml");
        assertTrue(generatedFile.exists());

        // cpd.xml should have been copied to the site
        File reportCpdXml = new File(outputDir, "cpd.xml");
        assertTrue(reportCpdXml.exists());
    }

    @InjectMojo(goal = "cpd", pom = "cpd-skip-empty-report-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @Test
    public void testSkipEmptyReportConfiguration(CpdReport mojo) throws Exception {
        mojo.execute();

        File outputDir = mojo.getReportOutputDirectory();
        String filename = mojo.getOutputPath() + ".html";

        File generatedReport = new File(outputDir, filename);
        assertFalse(new File(generatedReport.getAbsolutePath()).exists());

        // the (empty) cpd.xml file still needs to be created for the check mojo.
        File generatedFile = new File(getBasedir(), "target/test/unit/skip-empty-report-no-files/target/cpd.xml");
        assertTrue(generatedFile.exists());

        String output = CapturingPrintStream.getOutput();
        assertFalse(output.contains("[WARNING] No files to analyze.")); // this log message comes from PMD.
        assertTrue(output.contains("No files found to process. Skipping CPD execution."));

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
