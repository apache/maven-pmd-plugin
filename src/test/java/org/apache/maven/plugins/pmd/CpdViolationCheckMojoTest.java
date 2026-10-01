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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
@MojoTest
public class CpdViolationCheckMojoTest {

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd-check", pom = "cpd-check-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "cpd-report")
    @Test
    public void testDefaultConfiguration(CpdViolationCheckMojo mojo) throws Exception {
        try {
            mojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (final MojoFailureException e) {
            assertTrue(e.getMessage().startsWith("CPD " + AbstractPmdReport.getPmdVersion() + " has found 1 duplicat"));
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd-check", pom = "cpd-check-notfailonviolation-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "cpd-report")
    @Test
    public void testNotFailOnViolation(CpdViolationCheckMojo mojo) throws Exception {
        mojo.execute();
    }

    @Basedir("/unit/custom-configuration")
    @InjectMojo(goal = "cpd-check", pom = "cpd-check-exception-test-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "cpd-report")
    @Test
    public void testException(CpdViolationCheckMojo mojo) throws Exception {
        try {
            mojo.project = new MavenProject();
            mojo.execute();

            fail("MojoFailureException should be thrown.");
        } catch (MojoFailureException e) {
            assertTrue(e.getMessage().contains("Unable to perform check"));
        }
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd-check", pom = "cpd-check-cpd-exclusions-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "cpd-report")
    @Test
    public void testExclusionsConfiguration(CpdViolationCheckMojo mojo) throws Exception {
        // this call shouldn't throw an exception, as the classes with duplications have been excluded
        mojo.execute();
    }

    @Basedir("/unit/default-configuration")
    @InjectMojo(goal = "cpd-check", pom = "cpd-check-default-configuration-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "src/site")
    @MojoParameter(name = "targetDirectory", value = "cpd-report")
    @MojoParameter(name = "verbose", value = "true")
    @MojoParameter(name = "printFailingErrors", value = "true")
    @Test
    public void testVerboseTakesPrecedenceOverPrintFailingErrors(CpdViolationCheckMojo mojo) throws Exception {
        Log log = mock(Log.class);
        mojo.setLog(log);

        try {
            mojo.execute();
            fail("MojoFailureException expected");
        } catch (MojoFailureException expected) {
            ArgumentCaptor<CharSequence> messages = ArgumentCaptor.forClass(CharSequence.class);
            verify(log, atLeastOnce()).warn(messages.capture());
            List<CharSequence> violationMessages = messages.getAllValues().stream()
                    .filter(message -> message.toString().startsWith("CPD "))
                    .collect(Collectors.toList());
            assertEquals(
                    1,
                    violationMessages.stream()
                            .filter(message -> message.toString().startsWith("CPD Failure:"))
                            .count());
        }
    }
}
