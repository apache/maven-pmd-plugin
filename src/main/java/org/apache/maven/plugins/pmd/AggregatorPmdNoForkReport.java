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

import javax.inject.Inject;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.resource.ResourceManager;

/**
 * Creates a PMD site report in an <b>aggregator</b> project without forking the <code>test-compile</code> phase again.
 *
 * @since 3.15.0
 */
@Mojo(
        name = "aggregate-pmd-no-fork",
        aggregator = true,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.NONE)
public class AggregatorPmdNoForkReport extends AggregatorPmdReport {

    @Inject
    public AggregatorPmdNoForkReport(
            ToolchainManager toolchainManager,
            ResourceManager locator,
            DependencyResolver dependencyResolver,
            I18N i18n) {
        super(toolchainManager, locator, dependencyResolver, i18n);
    }
}
