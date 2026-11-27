<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Maven PMD Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-pmd-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-pmd-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/plugins/maven-pmd-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-pmd-plugin/README.md)
[![Verify](https://github.com/apache/maven-pmd-plugin/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-pmd-plugin/actions/workflows/maven-verify.yml)

Runs the [PMD](https://pmd.github.io/) static code analyzer and its Copy/Paste Detector (CPD) in a Maven build.
The `check` and `cpd-check` goals fail the build on rule violations or duplicated code; the `pmd` and `cpd`
goals, and their `aggregate-*` variants for multi-module builds, publish the results as site reports.

The plugin needs Maven 3.9.0 or later on Java 8 or later. Each release ships with a default PMD version, which
you can [override at runtime](https://maven.apache.org/plugins/maven-pmd-plugin/examples/upgrading-PMD-at-runtime.html).

- Documentation: <https://maven.apache.org/plugins/maven-pmd-plugin/>
- Issues: <https://github.com/apache/maven-pmd-plugin/issues>
- Release notes: <https://github.com/apache/maven-pmd-plugin/releases>

## Build

```shell
mvn verify
```

The integration tests under `src/it` run with the `run-its` profile:

```shell
mvn -Prun-its verify
```

To build the plugin site locally, including the goal pages:

```shell
mvn -Preporting site
```

## Contributing

Open an issue to discuss larger changes first, then send a pull request from a fork with tests
(a unit test or an integration test under `src/it`) and run `mvn -Prun-its verify` before you push.
See [Contributing to Apache Maven](https://maven.apache.org/guides/development/guide-helping.html) and the
[developer mailing list](https://maven.apache.org/mailing-lists.html).
