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

import javax.inject.Provider;

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract service executor for CPD and PMD.
 */
abstract class ServiceExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PmdExecutor.class);

    private final ToolchainManager toolchainManager;

    private final Provider<MavenSession> sessionProvider;

    protected ServiceExecutor(ToolchainManager toolchainManager, Provider<MavenSession> sessionProvider) {
        this.toolchainManager = toolchainManager;
        this.sessionProvider = sessionProvider;
    }

    protected final Toolchain getToolchain(Map<String, String> jdkToolchain) {
        Toolchain tc = null;

        if (jdkToolchain != null) {
            List<Toolchain> tcs = toolchainManager.getToolchains(sessionProvider.get(), "jdk", jdkToolchain);
            if (tcs != null && !tcs.isEmpty()) {
                tc = tcs.get(0);
            }
        }

        if (tc == null) {
            tc = toolchainManager.getToolchainFromBuildContext("jdk", sessionProvider.get());
        }

        return tc;
    }

    protected String getJavaExecutable(Map<String, String> jdkToolchain) {
        Toolchain tc = getToolchain(jdkToolchain);
        if (tc != null) {
            LOG.info("Toolchain in maven-pmd-plugin: {}", tc);
            return tc.findTool("java"); // NOI18N
        }
        return null;
    }
}
