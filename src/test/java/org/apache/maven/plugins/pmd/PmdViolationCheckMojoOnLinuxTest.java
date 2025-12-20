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

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
@MojoTest
@EnabledOnOs({LINUX, MAC})
public class PmdViolationCheckMojoOnLinuxTest {

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testDefaultConfiguration(PmdViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final MojoFailureException e) {
            assertTrue(
                    e.getMessage().startsWith("PMD " + AbstractPmdReport.getPmdVersion() + " has found 8 violations."));
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-notfailonviolation-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testNotFailOnViolation(PmdViolationCheckMojo mojo) throws Exception {

        mojo.execute();
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-notfailmaxviolation-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testNotFailMaxAllowedViolations(PmdViolationCheckMojo mojo) throws Exception {
        mojo.execute();
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-failmaxviolation-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testFailMaxAllowedViolations(PmdViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();
            fail("Exception Expected");
        } catch (final MojoFailureException e) {
            assertTrue(e.getMessage()
                    .startsWith("PMD " + AbstractPmdReport.getPmdVersion()
                            + " has found 5 violations and issued 3 warnings."));
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-failonpriority-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testFailurePriority(PmdViolationCheckMojo mojo) throws Exception {
        mojo.execute();
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-failandwarnonpriority-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testFailureAndWarningPriority(PmdViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();
            fail("MojoFailureException expected");
        } catch (final MojoFailureException e) {
            assertTrue(e.getMessage()
                    .startsWith("PMD " + AbstractPmdReport.getPmdVersion()
                            + " has found 5 violations and issued 3 warnings."));
        }
    }

    @Basedir("/unit/custom-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-exception-test-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testException(PmdViolationCheckMojo mojo) throws Exception {
        try {
            mojo.project = new MavenProject();
            mojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final MojoFailureException e) {
            assertTrue(e.getMessage().contains("Unable to perform check"));
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "check", pom = "pmd-check-pmd-exclusions-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "pmd-report")
    @Test
    public void testViolationExclusion(PmdViolationCheckMojo mojo) throws Exception {
        // this call shouldn't throw an exception, as the classes with violations have been excluded
        mojo.execute();
    }
}
