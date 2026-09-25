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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractPmdViolationCheckMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void verboseDoesNotPrintFailuresTwice() throws Exception {
        TestMojo mojo = new TestMojo();
        configure(mojo, true, true);

        mojo.executeCheck("pmd.xml", "PMD", "violation", 1);

        assertEquals(Arrays.asList("Warning:warning", "Failure:failure"), mojo.printed);
    }

    @Test
    void printFailingErrorsPrintsOnlyFailuresWhenNotVerbose() throws Exception {
        TestMojo mojo = new TestMojo();
        configure(mojo, false, true);

        mojo.executeCheck("pmd.xml", "PMD", "violation", 1);

        assertEquals(Arrays.asList("Failure:failure"), mojo.printed);
    }

    private void configure(TestMojo mojo, boolean verbose, boolean printFailingErrors) throws Exception {
        setField("targetDirectory", tempDir.toFile(), mojo);
        setField("verbose", verbose, mojo);
        setField("printFailingErrors", printFailingErrors, mojo);
        Files.createFile(tempDir.resolve("pmd.xml"));
    }

    private static void setField(String name, Object value, Object target) throws ReflectiveOperationException {
        Field field = AbstractPmdViolationCheckMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestMojo extends AbstractPmdViolationCheckMojo<String> {
        private final List<String> printed = new ArrayList<>();

        private TestMojo() {
            super(new ExcludeFromFile<String>() {
                @Override
                public void loadExcludeFromFailuresData(String excludeFromFailureFile) throws MojoExecutionException {}

                @Override
                public int countExclusions() {
                    return 0;
                }

                @Override
                public boolean isExcludedFromFailure(String errorDetail) {
                    return false;
                }
            });
            project = new org.apache.maven.project.MavenProject();
            failOnViolation = false;
        }

        @Override
        public void execute() {}

        @Override
        protected void printError(String item, String severity) {
            printed.add(severity + ":" + item);
        }

        @Override
        protected List<String> getErrorDetails(File analysisFile) throws XmlPullParserException, IOException {
            return Arrays.asList("failure", "warning");
        }

        @Override
        protected int getPriority(String errorDetail) {
            return "failure".equals(errorDetail) ? 1 : 2;
        }

        @Override
        protected ViolationDetails<String> newViolationDetailsInstance() {
            return new ViolationDetails<>();
        }
    }
}
