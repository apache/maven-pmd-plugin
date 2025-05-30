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

import org.codehaus.plexus.logging.console.ConsoleLogger;

abstract class Executor {

    protected static String buildClasspath() {
        StringBuilder classpath = new StringBuilder();

        // plugin classpath needs to come first
        ClassLoader pluginClassloader = Executor.class.getClassLoader();
        buildClasspath(classpath, pluginClassloader);

        ClassLoader coreClassloader = ConsoleLogger.class.getClassLoader();
        buildClasspath(classpath, coreClassloader);

        return classpath.toString();
    }

    static void buildClasspath(StringBuilder classpath, ClassLoader cl) {
        if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
                if ("file".equalsIgnoreCase(url.getProtocol())) {
                    try {
                        String filename = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());
                        classpath.append(new File(filename).getPath()).append(File.pathSeparatorChar);
                    } catch (UnsupportedEncodingException e) {
                        // skip as we provide the correct standard encoding
                    }
                }
            }
        }
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
