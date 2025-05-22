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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.toolchain.ToolchainManager;

/**
 * Service for executing CPD.
 */
@Named
@Singleton
public class CpdServiceExecutor extends ServiceExecutor {

    @Inject
    public CpdServiceExecutor(ToolchainManager toolchainManager, Provider<MavenSession> sessionProvider) {
        super(toolchainManager, sessionProvider);
    }

    public CpdResult execute(CpdRequest request) throws MavenReportException {

        ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CpdExecutor.class.getClassLoader());
            CpdExecutor executor = new CpdExecutor(request);

            String javaExecutable = getJavaExecutable(request.getJdkToolchain());
            if (javaExecutable != null) {
                return executor.fork(javaExecutable);
            }

            return executor.run();
        } finally {
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }
}
