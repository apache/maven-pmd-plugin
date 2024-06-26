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

import org.apache.maven.plugin.MojoFailureException;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class PmdViolationCheckMojoTest extends AbstractPmdReportTestCase {

    public void testDefaultConfiguration() throws Exception {
        generateReport("pmd", "default-configuration/default-configuration-plugin-config.xml");

        // clear the output from previous pmd:pmd execution
        CapturingPrintStream.init(true);

        try {
            final File testPom = new File(
                    getBasedir(),
                    "src/test/resources/unit/default-configuration/pmd-check-default-configuration-plugin-config.xml");
            final PmdViolationCheckMojo mojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);
            mojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final Exception e) {
            // the version should be logged
            String output = CapturingPrintStream.getOutput();
            assertTrue(output.contains("PMD version: " + AbstractPmdReport.getPmdVersion()));

            assertTrue(e.getMessage().startsWith("You have 8 PMD violations."));
        }
    }

    public void testNotFailOnViolation() throws Exception {
        generateReport("pmd", "default-configuration/default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/pmd-check-notfailonviolation-plugin-config.xml");
        final PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);
        pmdViolationMojo.execute();

        assertTrue(true);
    }

    public void testMaxAllowedViolations() throws Exception {
        generateReport("pmd", "default-configuration/default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/pmd-check-notfailmaxviolation-plugin-config.xml");
        final PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);
        pmdViolationMojo.execute();

        testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/pmd-check-failmaxviolation-plugin-config.xml");
        final PmdViolationCheckMojo pmdViolationMojoFail = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);

        try {
            pmdViolationMojoFail.execute();
            fail("Exception Expected");
        } catch (final MojoFailureException e) {
            String message = e.getMessage();
            if (message.contains("You have 5 PMD violations and 3 warnings.")) {
                System.out.println("Caught expected message: " + e.getMessage()); // expected
            } else {
                throw new AssertionError(
                        "Expected: '" + message + "' to contain 'You have 5 PMD violations and 3 warnings.'");
            }
        }
    }

    public void testFailurePriority() throws Exception {
        generateReport("pmd", "default-configuration/default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/pmd-check-failonpriority-plugin-config.xml");
        PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);
        pmdViolationMojo.execute();

        testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/pmd-check-failandwarnonpriority-plugin-config.xml");
        pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);
        try {
            pmdViolationMojo.execute();
            fail("Exception Expected");
        } catch (final MojoFailureException e) {
            String message = e.getMessage();
            if (message.contains("You have 5 PMD violations and 3 warnings.")) {
                System.out.println("Caught expected message: " + e.getMessage()); // expected
            } else {
                throw new AssertionError(
                        "Expected: '" + message + "' to contain 'You have 5 PMD violations and 3 warnings.'");
            }
        }
    }

    public void testException() throws Exception {
        try {
            final File testPom = new File(
                    getBasedir(),
                    "src/test/resources/unit/custom-configuration/pmd-check-exception-test-plugin-config.xml");
            final PmdViolationCheckMojo mojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);
            mojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final Exception e) {
            assertTrue(true);
        }
    }

    public void testViolationExclusion() throws Exception {
        generateReport("pmd", "default-configuration/default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/pmd-check-pmd-exclusions-configuration-plugin-config.xml");
        final PmdViolationCheckMojo pmdViolationMojo = (PmdViolationCheckMojo) lookupMojo(getGoal(), testPom);

        // this call shouldn't throw an exception, as the classes with violations have been excluded
        pmdViolationMojo.execute();
    }

    @Override
    protected String getGoal() {
        return "check";
    }
}
