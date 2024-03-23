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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sourceforge.pmd.cpd.Mark;
import net.sourceforge.pmd.cpd.Match;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.model.CpdFile;
import org.apache.maven.plugins.pmd.model.Duplication;

/**
 * This class contains utility methods to load property files which define which files
 * should be excluded from the CPD duplication results.
 * See property <code>pmd.excludeFromFailureFile</code>.
 *
 * @author Andreas Dangel
 */
public class ExcludeDuplicationsFromFile implements ExcludeFromFile<Duplication> {

    private final List<Set<String>> exclusionList = new ArrayList<>();

    @Override
    public boolean isExcludedFromFailure(final Duplication errorDetail) {
        final Set<String> uniquePaths = new HashSet<>();
        for (final CpdFile cpdFile : errorDetail.getFiles()) {
            uniquePaths.add(cpdFile.getPath());
        }
        return isExcludedFromFailure(uniquePaths);
    }

    /**
     * Checks whether the given {@link Match} is excluded.
     * Note: The exclusion must have been loaded before via {@link #loadExcludeFromFailuresData(String)}.
     *
     * @param errorDetail the duplication to check
     * @return <code>true</code> if the given duplication should be excluded, <code>false</code> otherwise.
     */
    public boolean isExcludedFromFailure(final Match errorDetail) {
        final Set<String> uniquePaths = new HashSet<>();
        for (Mark mark : errorDetail.getMarkSet()) {
            uniquePaths.add(mark.getLocation().getFileId().getAbsolutePath());
        }
        return isExcludedFromFailure(uniquePaths);
    }

    private boolean isExcludedFromFailure(Set<String> uniquePaths) {
        for (final Set<String> singleExclusionGroup : exclusionList) {
            if (uniquePaths.size() == singleExclusionGroup.size()
                    && duplicationExcludedByGroup(uniquePaths, singleExclusionGroup)) {
                return true;
            }
        }
        return false;
    }

    private boolean duplicationExcludedByGroup(final Set<String> uniquePaths, final Set<String> singleExclusionGroup) {
        for (final String path : uniquePaths) {
            if (!fileExcludedByGroup(path, singleExclusionGroup)) {
                return false;
            }
        }
        return true;
    }

    private boolean fileExcludedByGroup(final String path, final Set<String> singleExclusionGroup) {
        final String formattedPath = path.replace('\\', '.').replace('/', '.');
        for (final String className : singleExclusionGroup) {
            if (formattedPath.contains(className)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void loadExcludeFromFailuresData(final String excludeFromFailureFile) throws MojoExecutionException {
        if (excludeFromFailureFile == null || excludeFromFailureFile.isEmpty()) {
            return;
        }

        try (Stream<String> lines = Files.lines(Paths.get(excludeFromFailureFile))) {
            exclusionList.addAll(lines.filter(line -> !line.startsWith("#"))
                    .map(line -> createSetFromExclusionLine(line))
                    .collect(Collectors.toList()));
        } catch (final IOException e) {
            throw new MojoExecutionException("Cannot load file " + excludeFromFailureFile, e);
        }
    }

    private Set<String> createSetFromExclusionLine(final String line) {
        final Set<String> result = new HashSet<>();
        for (final String className : line.split(",")) {
            result.add(className.trim());
        }
        return result;
    }

    @Override
    public int countExclusions() {
        return exclusionList.size();
    }
}
