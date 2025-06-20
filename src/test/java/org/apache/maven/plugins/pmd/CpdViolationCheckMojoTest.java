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
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class CpdViolationCheckMojoTest extends AbstractPmdReportTestCase {

    public void testDefaultConfiguration() throws Exception {
        generateReport("cpd", "default-configuration/cpd-default-configuration-plugin-config.xml");

        try {
            File testPom = new File(
                    getBasedir(),
                    "src/test/resources/unit/default-configuration/cpd-check-default-configuration-plugin-config.xml");
            CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo(getGoal(), testPom);
            cpdViolationCheckMojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final MojoFailureException e) {
            assertTrue(e.getMessage().startsWith("CPD " + AbstractPmdReport.getPmdVersion() + " has found 1 duplicat"));
        }
    }

    public void testNotFailOnViolation() throws Exception {
        generateReport("cpd", "default-configuration/cpd-default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/cpd-check-notfailonviolation-plugin-config.xml");
        CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo(getGoal(), testPom);
        cpdViolationCheckMojo.execute();
    }

    public void testException() throws Exception {
        try {
            File testPom = new File(
                    getBasedir(),
                    "src/test/resources/unit/custom-configuration/cpd-check-exception-test-plugin-config.xml");
            CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo(getGoal(), testPom);
            cpdViolationCheckMojo.project = new MavenProject();
            cpdViolationCheckMojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (MojoFailureException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testExclusionsConfiguration() throws Exception {
        generateReport("cpd", "default-configuration/cpd-default-configuration-plugin-config.xml");

        File testPom = new File(
                getBasedir(),
                "src/test/resources/unit/default-configuration/cpd-check-cpd-exclusions-configuration-plugin-config.xml");
        CpdViolationCheckMojo cpdViolationCheckMojo = lookupMojo(getGoal(), testPom);

        // this call shouldn't throw an exception, as the classes with duplications have been excluded
        cpdViolationCheckMojo.execute();
    }

    @Override
    protected String getGoal() {
        return "cpd-check";
    }
}
