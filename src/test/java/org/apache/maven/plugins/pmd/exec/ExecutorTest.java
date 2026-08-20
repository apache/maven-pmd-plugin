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
package org.apache.maven.plugins.pmd.exec;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecutorTest {
    @Test
    public void testBuildClasspath() throws MalformedURLException {
        String basename = "home/test/dir with space/mylib.jar";
        String pathname = new File("/", basename).getPath();
        if (SystemUtils.IS_OS_WINDOWS) {
            pathname = new File(File.listRoots()[0], basename).getPath();
        }
        URL[] urls = new URL[] {new File(pathname).toURI().toURL()};
        URLClassLoader mockedClassLoader = new URLClassLoader(urls);

        List<String> classpath = Executor.buildClasspath(mockedClassLoader);
        assertEquals(Collections.singletonList(pathname), classpath);
    }

    @ParameterizedTest
    @CsvSource({" 8, '1.8'", "11, '11'", "17, '17-ea'", "25, '25'"})
    public void parseMajorJavaVersion(int expectedVersion, String value) {
        assertEquals(
                expectedVersion, Executor.parseJavaVersion("xx\njava.specification.version = " + value + "\nyy\n"));
    }

    @Test
    public void parseUnknownMajorJavaVersion() {
        assertEquals(-1, Executor.parseJavaVersion("xx\nyy\n"));
    }
}
