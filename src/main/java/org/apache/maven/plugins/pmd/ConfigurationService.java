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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

/**
 * Utils service for provide configuration needed to execute CPD/PMD.
 */
@Named
@Singleton
public class ConfigurationService {

    private final Provider<MavenSession> sessionProvider;

    private final org.eclipse.aether.RepositorySystem repositorySystem;

    @Inject
    public ConfigurationService(
            Provider<MavenSession> sessionProvider, org.eclipse.aether.RepositorySystem repositorySystem) {
        this.sessionProvider = sessionProvider;
        this.repositorySystem = repositorySystem;
    }

    public List<File> resolveDependenciesAsFile(
            MavenProject localProject, Collection<MavenProject> aggregatedProjects, boolean includeTests)
            throws DependencyResolutionException {

        RepositorySystemSession repositorySession = sessionProvider.get().getRepositorySession();
        ArtifactTypeRegistry artifactTypeRegistry = repositorySession.getArtifactTypeRegistry();

        List<String> includesScope =
                includeTests ? Arrays.asList("compile", "provided", "test") : Arrays.asList("compile", "provided");

        // collect exclusions for projects within the reactor
        // if module a depends on module b and both are in the reactor
        // then we don't want to resolve the dependency as an artifact.
        List<String> exclusionPatterns = new ArrayList<>();
        for (MavenProject project : aggregatedProjects) {
            exclusionPatterns.add(getExclusionKey(project));
        }

        List<org.eclipse.aether.graph.Dependency> dependencies = localProject.getDependencies().stream()
                .filter(dependency -> includesScope.contains(dependency.getScope()))
                .filter(dependency -> !exclusionPatterns.contains(getExclusionKey(dependency)))
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toList());

        List<org.eclipse.aether.graph.Dependency> dependencyManagements = Optional.ofNullable(
                        localProject.getDependencyManagement())
                .map(DependencyManagement::getDependencies)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toList());

        CollectRequest collectRequest =
                new CollectRequest(dependencies, dependencyManagements, localProject.getRemoteProjectRepositories());
        DependencyRequest request = new DependencyRequest(collectRequest, null);

        DependencyResult result =
                repositorySystem.resolveDependencies(sessionProvider.get().getRepositorySession(), request);

        return result.getArtifactResults().stream()
                .map(ArtifactResult::getArtifact)
                .map(Artifact::getFile)
                .collect(Collectors.toList());
    }

    private String getExclusionKey(Dependency dependency) {
        return dependency.getGroupId() + ":" + dependency.getArtifactId();
    }

    private String getExclusionKey(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }
}
