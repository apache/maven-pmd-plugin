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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;

abstract class Executor {

    protected static String buildClasspath(String javaExecutable) {
        List<String> classpath = new ArrayList<>();

        // plugin classpath needs to come first
        ClassLoader pluginClassloader = Executor.class.getClassLoader();
        classpath.addAll(buildClasspath(pluginClassloader));

        ClassLoader coreClassloader = ConsoleLogger.class.getClassLoader();
        classpath.addAll(buildClasspath(coreClassloader));

        // Determine Java version: When running under maven4 and a toolchain
        // selects a jdk < 17, then we need to exclude "maven-logging-x.jar" from the
        // classpath. Otherwise slf4j's Logging Factory will try to initiale Maven4's
        // service provider (org.apache.maven.slf4j.MavenServiceProvider) which will
        // lead to a UnsupportedClassVersionError.
        int majorJavaVersion = determineJavaVersion(javaExecutable);
        if (majorJavaVersion < 17) {
            classpath.removeIf(s -> s.contains("maven-logging"));
        }

        return classpath.stream().collect(Collectors.joining(File.pathSeparator));
    }

    private static int determineJavaVersion(String javaExecutable) {
        ProcessBuilder pb = new ProcessBuilder(javaExecutable, "-XshowSettings:properties", "-version");
        pb.redirectErrorStream(true);
        Process java = null;
        try {
            java = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't run java to determine java version", e);
        }

        try (InputStream in = java.getInputStream()) {
            String properties = IOUtil.toString(in);
            return parseJavaVersion(properties);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't determine java version", e);
        }
    }

    static int parseJavaVersion(String properties) {
        Pattern versionPattern = Pattern.compile("java\\.specification\\.version\\s*=\\s*(?:1\\.)?(\\d+)");
        Matcher versionMatcher = versionPattern.matcher(properties);
        if (versionMatcher.find()) {
            String major = versionMatcher.group(1); // e.g. "8", "11, "17", ...
            return Integer.parseInt(major);
        }
        return -1;
    }

    static List<String> buildClasspath(ClassLoader cl) {
        List<String> classpath = new ArrayList<>();
        if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
                if ("file".equalsIgnoreCase(url.getProtocol())) {
                    try {
                        String filename = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());
                        classpath.add(new File(filename).getPath());
                    } catch (UnsupportedEncodingException e) {
                        // skip as we provide the correct standard encoding
                    }
                }
            }
        }
        return classpath;
    }

    protected static class ProcessStreamHandler implements Runnable {
        private static final int BUFFER_SIZE = 8192;

        private final BufferedInputStream in;
        private final BufferedOutputStream out;

        public static void start(InputStream in, OutputStream out) {
            Thread t = new Thread(new ProcessStreamHandler(in, out));
            t.start();
        }

        private ProcessStreamHandler(InputStream in, OutputStream out) {
            this.in = new BufferedInputStream(in);
            this.out = new BufferedOutputStream(out);
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                int count = in.read(buffer);
                while (count != -1) {
                    out.write(buffer, 0, count);
                    out.flush();
                    count = in.read(buffer);
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
