<!--

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

-->

<!--
Header Templates
### U+1F680 (Rocket)  New features and improvements
### U+1F41B (Bug)     Bug Fixes
### U+1F47B (Ghost)   Maintenance
### U+1F4DD (Memo)    Documentation updates
### U+1F4E6 (Package) Dependency updates
-->

# Release Notes

## Version 3.24.0

**Release Date:** 2024-07-10

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.24.0](https://issues.apache.org/jira/projects/MPMD/versions/12354924)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.24.0>

### 🚀 New features and improvements
* [MPMD-391](https://issues.apache.org/jira/browse/MPMD-391) - Log what developers care about and not what they don't ([#156](https://github.com/apache/maven-pmd-plugin/pull/156)) @michael-o

### 🐛 Bug Fixes
* [MPMD-399](https://issues.apache.org/jira/browse/MPMD-399) - Incorrect warning: The project X does not seem to be compi… ([#154](https://github.com/apache/maven-pmd-plugin/pull/154)) @michael-o

### 📦 Dependency updates
* [MPMD-400](https://issues.apache.org/jira/browse/MPMD-400) - Upgrade to PMD 7.3.0 ([#157](https://github.com/apache/maven-pmd-plugin/pull/157)) @michael-o
* Bump org.codehaus.mojo:animal-sniffer-maven-plugin from 1.23 to 1.24 ([#155](https://github.com/apache/maven-pmd-plugin/pull/155)) @dependabot
* Bump org.apache.maven.shared:maven-common-artifact-filters from 3.3.2 to 3.4.0 ([#153](https://github.com/apache/maven-pmd-plugin/pull/153)) @dependabot

### 👻 Maintenance
* Remove outdated ([#158](https://github.com/apache/maven-pmd-plugin/pull/158)) @michael-o

## Version 3.23.0

**Release Date:** 2024-06-08

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.23.0](https://issues.apache.org/jira/projects/MPMD/versions/12354616)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.23.0>

### 🐛 Bug Fixes
* [MPMD-395](https://issues.apache.org/jira/browse/MPMD-395) - Build doesn't fail for invalid CPD format ([#150](https://github.com/apache/maven-pmd-plugin/pull/150)) @adangel

### 📦 Dependency updates
* [MPMD-397](https://issues.apache.org/jira/browse/MPMD-397) - Upgrade to Maven 3.6.3 ([#151](https://github.com/apache/maven-pmd-plugin/pull/151)) @michael-o

## Version 3.22.0

**Release Date:** 2024-04-18

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.22.0](https://issues.apache.org/jira/projects/MPMD/versions/12353876)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.22.0>

### 🚀 New features and improvements
* [MPMD-379](https://issues.apache.org/jira/browse/MPMD-379) - Upgrade to use PMD 7.0.0 by default ([#144](https://github.com/apache/maven-pmd-plugin/pull/144)) @mkolesnikov

### 📦 Dependency updates
* [MPMD-394](https://issues.apache.org/jira/browse/MPMD-394) - Bump org.apache.maven.plugins:maven-plugins from 41 to 42 ([#148](https://github.com/apache/maven-pmd-plugin/pull/148)) @dependabot
* [MPMD-393](https://issues.apache.org/jira/browse/MPMD-393) - Bump commons-io:commons-io from 2.16.0 to 2.16.1 ([#147](https://github.com/apache/maven-pmd-plugin/pull/147)) @dependabot
* [MPMD-393](https://issues.apache.org/jira/browse/MPMD-393) - Bump commons-io:commons-io from 2.15.1 to 2.16.0 ([#146](https://github.com/apache/maven-pmd-plugin/pull/146)) @dependabot
* Bump apache/maven-gh-actions-shared from 3 to 4 ([#143](https://github.com/apache/maven-pmd-plugin/pull/143)) @dependabot
* Bump org.codehaus.plexus:plexus-resources from 1.2.0 to 1.3.0 ([#140](https://github.com/apache/maven-pmd-plugin/pull/140)) @dependabot
* Bump org.apache.commons:commons-lang3 from 3.12.0 to 3.14.0 ([#137](https://github.com/apache/maven-pmd-plugin/pull/137)) @dependabot
* Bump commons-io:commons-io from 2.11.0 to 2.15.1 ([#138](https://github.com/apache/maven-pmd-plugin/pull/138)) @dependabot
* [MPMD-388](https://issues.apache.org/jira/browse/MPMD-388) - Upgrade to Parent 41 @michael-o

### 👻 Maintenance
* Bump release-drafter/release-drafter from 5 to 6 ([#142](https://github.com/apache/maven-pmd-plugin/pull/142)) @dependabot

## Version 3.21.2

**Release Date:** 2023-10-30

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.21.2](https://issues.apache.org/jira/projects/MPMD/versions/12353789)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.21.2>

### 🐛 Bug Fixes
* [MPMD-370](https://issues.apache.org/jira/browse/MPMD-370) - Remove remaining uses of FileReader
* [MPMD-371](https://issues.apache.org/jira/browse/MPMD-371) - Using two ruleset files with same name in different directories ([#127](https://github.com/apache/maven-pmd-plugin/pull/127)) @harbulot
* [MPMD-382](https://issues.apache.org/jira/browse/MPMD-382) - Regression in report rendering
* [MPMD-384](https://issues.apache.org/jira/browse/MPMD-384) - maven-pmd-plugin is dowloading transitive dependencies of unmanaged version ([#135](https://github.com/apache/maven-pmd-plugin/pull/135)) @caiwei-ebay

### 👻 Maintenance
* Move commons-lang3 to test scope ([#131](https://github.com/apache/maven-pmd-plugin/pull/131)) @elharo

### 📝 Documentation updates
* Typo: fill --> file ([#133](https://github.com/apache/maven-pmd-plugin/pull/133)) @elharo

### 📦 Dependency updates
* [MPMD-380](https://issues.apache.org/jira/browse/MPMD-380) - Prefer apache commons and JDK to Plexus utils ([#132](https://github.com/apache/maven-pmd-plugin/pull/132)) @elharo
* [MPMD-387](https://issues.apache.org/jira/browse/MPMD-387) - Upgrade to Parent 40

## Version 3.21.0

**Release Date:** 2023-05-12

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.21.0](https://issues.apache.org/jira/projects/MPMD/versions/12352863)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.21.0>

### 🚀 New features and improvements
* [MPMD-365](https://issues.apache.org/jira/browse/MPMD-365) - Support Java 20 ([#116](https://github.com/apache/maven-pmd-plugin/pull/116)) @adangel
* [MPMD-375](https://issues.apache.org/jira/browse/MPMD-375) - Replace *ReportGenerators with new *ReportRenderers ([#130](https://github.com/apache/maven-pmd-plugin/pull/130)) @michael-o

### 🐛 Bug Fixes
* [MPMD-369](https://issues.apache.org/jira/browse/MPMD-369) - System encoding conflicts with XML encoding in CpdViolationCheckMojo ([#122](https://github.com/apache/maven-pmd-plugin/pull/122)) @elharo
* [MPMD-373](https://issues.apache.org/jira/browse/MPMD-373) - System property java.version is overwritten in ITs as model property ([#128](https://github.com/apache/maven-pmd-plugin/pull/128)) @michael-o

### 👻 Maintenance
* [MPMD-374](https://issues.apache.org/jira/browse/MPMD-374) - Remove deprecated and unused PmdCollectingRenderer ([#128](https://github.com/apache/maven-pmd-plugin/pull/128)) @michael-o
* [MNG-6829](https://issues.apache.org/jira/browse/MNG-6829) - Replace any StringUtils#isEmpty(String) and #isNotEmpty(String) ([#124](https://github.com/apache/maven-pmd-plugin/pull/124)) @timtebeek
* Remove vestigial useJava5 parameter ([#119](https://github.com/apache/maven-pmd-plugin/pull/119)) @elharo
* [MPMD-367](https://issues.apache.org/jira/browse/MPMD-367) - Verify / ubuntu-latest jdk-11-temurin 3.9.1 broken at head in MPMD<!-- -->-270-325-JDK11 ([#121](https://github.com/apache/maven-pmd-plugin/pull/121)) @elharo

### 📦 Dependency updates
* [MPMD-364](https://issues.apache.org/jira/browse/MPMD-364) - Upgrade to PMD 6.55.0 ([#115](https://github.com/apache/maven-pmd-plugin/pull/115)) @dependabot
* [MPMD-364](https://issues.apache.org/jira/browse/MPMD-364) - Upgrade to PMD 6.54.0 ([#112](https://github.com/apache/maven-pmd-plugin/pull/112)) @adangel
* [MPMD-366](https://issues.apache.org/jira/browse/MPMD-366) - Update parent pom to 39 ([#118](https://github.com/apache/maven-pmd-plugin/pull/118)) @elharo
* [MPMD-372](https://issues.apache.org/jira/browse/MPMD-372) - Upgrade plugins and components (in ITs) ([#128](https://github.com/apache/maven-pmd-plugin/pull/128)) @michael-o
* Bump animal-sniffer-maven-plugin from 1.22 to 1.23 ([#117](https://github.com/apache/maven-pmd-plugin/pull/117)) @dependabot
* Bump release-drafter/release-drafter from 5.21.1 to 5.23.0 ([#114](https://github.com/apache/maven-pmd-plugin/pull/114)) @dependabot
* Bump apache/maven-gh-actions-shared from 2 to 3 ([#113](https://github.com/apache/maven-pmd-plugin/pull/113)) @dependabot
* Bump doxia-sink-api from 1.11.1 to 1.12.0 ([#111](https://github.com/apache/maven-pmd-plugin/pull/111)) @dependabot
* Bump wagon-http-lightweight from 3.5.2 to 3.5.3 ([#108](https://github.com/apache/maven-pmd-plugin/pull/108)) @dependabot

## Version 3.20.0

**Release Date:** 2023-01-06

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.20.0](https://issues.apache.org/jira/projects/MPMD/versions/12352270)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.20.0>

### 🐛 Bug Fixes
* [MPMD-335](https://issues.apache.org/jira/browse/MPMD-335) - Aggregate mode doesn't use additional repositories ([#101](https://github.com/apache/maven-pmd-plugin/pull/101)) @adangel

### 👻 Maintenance
* [MPMD-361](https://issues.apache.org/jira/browse/MPMD-361) - Explicitly start and end tables with Doxia Sinks in report renderers

### 📦 Dependency updates
* [MPMD-360](https://issues.apache.org/jira/browse/MPMD-360) - Upgrade to PMD 6.53.0 ([#109](https://github.com/apache/maven-pmd-plugin/pull/109)) @adangel
* [MPMD-358](https://issues.apache.org/jira/browse/MPMD-358) - Upgrade to PMD 6.52.0 ([#104](https://github.com/apache/maven-pmd-plugin/pull/104)) @adangel
* [MPMD-357](https://issues.apache.org/jira/browse/MPMD-357) - Upgrade to PMD 6.51.0 ([#100](https://github.com/apache/maven-pmd-plugin/pull/100)) @adangel
* Bump release-drafter/release-drafter from 5.21.0 to 5.21.1 ([#99](https://github.com/apache/maven-pmd-plugin/pull/99)) @dependabot
* [MPMD-356](https://issues.apache.org/jira/browse/MPMD-356) - Upgrade to PMD 6.50.0 ([#98](https://github.com/apache/maven-pmd-plugin/pull/98)) @adangel
* Bump maven-common-artifact-filters from 3.3.1 to 3.3.2 ([#95](https://github.com/apache/maven-pmd-plugin/pull/95)) @dependabot
* Bump release-drafter/release-drafter from 5.20.1 to 5.21.0 ([#93](https://github.com/apache/maven-pmd-plugin/pull/93)) @dependabot

## Version 3.19.0

**Release Date:** 2022-09-01

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.19.0](https://issues.apache.org/jira/projects/MPMD/versions/12352255)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.19.0>

### 🐛 Bug Fixes
* [MPMD-353](https://issues.apache.org/jira/browse/MPMD-353) - API incompatibility with jansi after upgrading m-shared-utils (#91) @adangel

### 📦 Dependency updates
* Bump animal-sniffer-maven-plugin from 1.21 to 1.22 (#88) @dependabot
* Bump wiremock from 1.49 to 2.27.2 (#57) @dependabot
* [MPMD-354](https://issues.apache.org/jira/browse/MPMD-354) - Upgrade to PMD 6.49.0 (#92) @adangel
* Bump release-drafter/release-drafter from 5.20.0 to 5.20.1 (#86) @dependabot

## Version 3.18.0

**Release Date:** 2022-08-20

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.18.0](https://issues.apache.org/jira/projects/MPMD/versions/12351813)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.18.0>

### 🚀 New features and improvements
* [MPMD-348](https://issues.apache.org/jira/browse/MPMD-348) - Support Java 19 (#82) @adangel

### 🐛 Bug Fixes
* [SECURITY] Fix Partial Path Traversal Vulnerability (#80) @JLLeitschuh

### 📦 Dependency updates
* [MPMD-352](https://issues.apache.org/jira/browse/MPMD-352) - Upgrade Maven Common Artifact Filters to 3.3.1
* [MPMD-351](https://issues.apache.org/jira/browse/MPMD-351) - Upgrade Maven Artifact Transfer to 0.13.1
* [MPMD-350](https://issues.apache.org/jira/browse/MPMD-350) - Upgrade Maven Shared Utils to 3.3.4
* [MPMD-349](https://issues.apache.org/jira/browse/MPMD-349) - Upgrade Maven Reporting API to 3.1.1/Maven Reporting Impl to 3.2.0 (#84) @michael-o
* [MPMD-347](https://issues.apache.org/jira/browse/MPMD-347) - Upgrade to PMD 6.48.0 (#81) @adangel
* Bump maven-plugins from 36 to 37 (#79) @dependabot
* [MPMD-345](https://issues.apache.org/jira/browse/MPMD-345) - Upgrade to PMD 6.47.0 (#73) @adangel
* Bump commons-lang3 from 3.8.1 to 3.12.0 (#72) @dependabot
* Bump plexus-resources from 1.1.0 to 1.2.0 (#56) @dependabot
* Bump animal-sniffer-maven-plugin from 1.16 to 1.21 (#54) @dependabot

### 💥 Compatibility Notice

For technical reasons the parameter `sourceEncoding` has been replaced with `inputEncoding`.
For details please see [MPMD-349](https://issues.apache.org/jira/browse/MPMD-349)/[2b7d2d7065bae1f984c82d210062064376fbd430](https://gitbox.apache.org/repos/asf?p=maven-pmd-plugin.git;a=commit;h=2b7d2d7065bae1f984c82d210062064376fbd430).

## Version 3.17.0

**Release Date:** 2022-05-31

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.17.0](https://issues.apache.org/jira/projects/MPMD/versions/12351350)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.17.0>

### 🚀 New features and improvements
* [MPMD-309](https://issues.apache.org/jira/browse/MPMD-309) - Add configuration option to show suppressed violations (#59) @adangel
* [MPMD-332](https://issues.apache.org/jira/browse/MPMD-332) - Support Java 18 (#63) @adangel

### 🐛 Bug Fixes
* [MPMD-334](https://issues.apache.org/jira/browse/MPMD-334) - Source Encoding parameter is ignored (#64) @laoseth
* [MPMD-342](https://issues.apache.org/jira/browse/MPMD-342) - No debug log message issued when empty report shall be ski… (#69) @michael-o

### 📝 Documentation updates
* [MPMD-333](https://issues.apache.org/jira/browse/MPMD-333) - Add release notes documentation (#61) @adangel

### 👻 Maintenance
* [MPMD-336](https://issues.apache.org/jira/browse/MPMD-336) - Replace deprecated calls to PMD (#66) @adangel

### 📦 Dependency updates
* [MPMD-329](https://issues.apache.org/jira/browse/MPMD-329) - Upgrade to PMD 6.45.0
* [MPMD-330](https://issues.apache.org/jira/browse/MPMD-330) - Upgrade Maven Parent to 35 (#60) @adangel
* [MPMD-331](https://issues.apache.org/jira/browse/MPMD-331) - Require Maven 3.2.5+ (#60) @adangel
* [MPMD-337](https://issues.apache.org/jira/browse/MPMD-337) - Upgrade Maven Parent to 36 (#67) @adangel
* [MPMD-338](https://issues.apache.org/jira/browse/MPMD-338) - Upgrade to Doxia/Doxia Sitetools to 1.11.1
* [MPMD-339](https://issues.apache.org/jira/browse/MPMD-339) - Upgrade plugins in ITs
* [MPMD-340](https://issues.apache.org/jira/browse/MPMD-340) - Upgrade Maven Reporting API/Impl to 3.1.0
* [MPMD-341](https://issues.apache.org/jira/browse/MPMD-341) - Upgrade Maven Plugin Test Harness to 3.3.0
* [MPMD-343](https://issues.apache.org/jira/browse/MPMD-343) - Upgrade to PMD 6.46.0 (#71) @adangel
* Bump release-drafter/release-drafter from 5.19.0 to 5.20.0 (#68) @dependabot
* Bump release-drafter/release-drafter from 5.18.1 to 5.19.0 (#58) @dependabot
* Bump commons-io from 2.6 to 2.7 in /src/it/MPMD-318-auxclasspath-includeTests/module-a (#65) @dependabot
* Bump slf4jVersion from 1.7.25 to 1.7.36 (#53) @dependabot

## Version 3.16.0

**Release Date:** 2022-02-05

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.16.0](https://issues.apache.org/jira/projects/MPMD/versions/12350599)

**GitHub:** <https://github.com/apache/maven-pmd-plugin/releases/tag/maven-pmd-plugin-3.16.0>

### 🐛 Bug Fixes
* [MPMD-325](https://issues.apache.org/jira/browse/MPMD-325) - Could not find class due to IncompatibleClassChangeError warning
* [MPMD-324](https://issues.apache.org/jira/browse/MPMD-324) - Ruleset URLs have hyphen replaced with forwardslash
* [MPMD-323](https://issues.apache.org/jira/browse/MPMD-323) - ResourceManager should use project base dir instead of pom location

### 🔧 Build
* [MPMD-328](https://issues.apache.org/jira/browse/MPMD-328) - Shared GitHub Actions

### 🚀 New features and improvements
* [MPMD-327](https://issues.apache.org/jira/browse/MPMD-327) - Upgrade to PMD 6.42.0

### 📦 Dependency updates
* [MPMD-326](https://issues.apache.org/jira/browse/MPMD-326) - Set Maven 3.1.1 as minimum version

## Version 3.15.0

**Release Date:** 2021-09-06

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.15.0](https://issues.apache.org/jira/projects/MPMD/versions/12349432)

### 🐛 Bug Fixes
* [MPMD-314](https://issues.apache.org/jira/browse/MPMD-314) - PMD report extension not set correctly for custom report class
* [MPMD-315](https://issues.apache.org/jira/browse/MPMD-315) - Maven PMD Plugin fails on Java 16: Unsupported targetJdk value '16'.
* [MPMD-317](https://issues.apache.org/jira/browse/MPMD-317) - NoClassDefFoundError for provided classes
* [MPMD-318](https://issues.apache.org/jira/browse/MPMD-318) - Incorrect aux classpath if 'includeTests' set to true
* [MPMD-320](https://issues.apache.org/jira/browse/MPMD-320) - Error when using toolchain and spaces in repository path

### 🚀 New features and improvements
* [MPMD-283](https://issues.apache.org/jira/browse/MPMD-283) - Create a real aggregate goal
* [MPMD-311](https://issues.apache.org/jira/browse/MPMD-311) - Improve excludeFromFailureFile docs
* [MPMD-313](https://issues.apache.org/jira/browse/MPMD-313) - Improve &lt;jdkToolchain&gt; parameter description
* [MPMD-321](https://issues.apache.org/jira/browse/MPMD-321) - Display PMD version that is being used also for pmd:pmd and pmd:cpd
* [MPMD-322](https://issues.apache.org/jira/browse/MPMD-322) - Display when PMD/CPD is skipped

### 🔧 Build
* [MPMD-319](https://issues.apache.org/jira/browse/MPMD-319) - Add GitHub Action to confirm build PR

### 📦 Dependency updates
* [MPMD-308](https://issues.apache.org/jira/browse/MPMD-308) - Set Maven 3.1.0 as minimum version
* [MPMD-316](https://issues.apache.org/jira/browse/MPMD-316) - Require Java 8
* [MPMD-312](https://issues.apache.org/jira/browse/MPMD-312) - Upgrade to PMD 6.38.0

## Version 3.14.0

**Release Date:** 2020-10-24

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.14.0](https://issues.apache.org/jira/projects/MPMD/versions/12346940)

### 🐛 Bug Fixes
* [MPMD-297](https://issues.apache.org/jira/browse/MPMD-297) - Classloader not being closed after PMD run
* [MPMD-300](https://issues.apache.org/jira/browse/MPMD-300) - Unable to format output with custom renderer (CodeClimateRenderer)
* [MPMD-305](https://issues.apache.org/jira/browse/MPMD-305) - CPD goal does not support txt as a format parameter
* [MPMD-307](https://issues.apache.org/jira/browse/MPMD-307) - NPE when using custom rule

### 🚀 New features and improvements
* [MPMD-290](https://issues.apache.org/jira/browse/MPMD-290) - Add CPD example for C#
* [MPMD-301](https://issues.apache.org/jira/browse/MPMD-301) - make build Reproducible
* [MPMD-304](https://issues.apache.org/jira/browse/MPMD-304) - maven-pmd-plugin should be toolchains-aware

### 📦 Dependency updates
* [MPMD-298](https://issues.apache.org/jira/browse/MPMD-298) - Upgrade Doxia Sitetools to 1.9.2 to remove dependency on Struts
* [MPMD-302](https://issues.apache.org/jira/browse/MPMD-302) - Upgrade to PMD 6.29.0

## Version 3.13.0

**Release Date:** 2020-01-25

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.13.0](https://issues.apache.org/jira/projects/MPMD/versions/12345409)

### 🐛 Bug Fixes
* [MPMD-288](https://issues.apache.org/jira/browse/MPMD-288) - NullPointerException when File.list() returns null
* [MPMD-289](https://issues.apache.org/jira/browse/MPMD-289) - check: unable to find pmd.xml
* [MPMD-292](https://issues.apache.org/jira/browse/MPMD-292) - PMD Log is not always integrated into maven log
* [MPMD-295](https://issues.apache.org/jira/browse/MPMD-295) - Maven PMD Plugin fails on Java 13: Unsupported targetJdk value '13'

### 🚀 New features and improvements
* [MPMD-225](https://issues.apache.org/jira/browse/MPMD-225) - Create  report even if no warnings have been found by default
* [MPMD-269](https://issues.apache.org/jira/browse/MPMD-269) - Display PMD version that is being used
* [MPMD-296](https://issues.apache.org/jira/browse/MPMD-296) - Copy ruleset files into a subdirectory of target

### 📝 Documentation updates
* [MPMD-241](https://issues.apache.org/jira/browse/MPMD-241) - Document the version relationship between plugin and pmd
* [MPMD-287](https://issues.apache.org/jira/browse/MPMD-287) - Add additional contribution information for GitHub

### 🔧 Build
* [MPMD-285](https://issues.apache.org/jira/browse/MPMD-285) - remove pluginTools version override for build (which block reproducible build...)
* [MPMD-293](https://issues.apache.org/jira/browse/MPMD-293) - Fix integration test builds on jenkins

### 📦 Dependency updates
* [MPMD-291](https://issues.apache.org/jira/browse/MPMD-291) - Upgrade to PMD 6.21.0

## Version 3.12.0

**Release Date:** 2019-04-11

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.12.0](https://issues.apache.org/jira/projects/MPMD/versions/12344380)

### 🐛 Bug Fixes
* [MPMD-277](https://issues.apache.org/jira/browse/MPMD-277) - Plugin tries to download local submodules from repo

### 🚀 New features and improvements
* [MPMD-280](https://issues.apache.org/jira/browse/MPMD-280) - Support targetJdk 12
* [MPMD-281](https://issues.apache.org/jira/browse/MPMD-281) - Display found violations grouped by priority
* [MPMD-282](https://issues.apache.org/jira/browse/MPMD-282) - Add rule name to HTML report

### 📝 Documentation updates
* [MPMD-279](https://issues.apache.org/jira/browse/MPMD-279) - Improve documentation of maxAllowedViolations

### 📦 Dependency updates
* [MPMD-275](https://issues.apache.org/jira/browse/MPMD-275) - Upgrade to PMD 6.13.0
* [MPMD-284](https://issues.apache.org/jira/browse/MPMD-284) - Upgrade parent to 33

## Version 3.11.0

**Release Date:** 2018-10-23

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.11.0](https://issues.apache.org/jira/projects/MPMD/versions/12343406)

### 🐛 Bug Fixes
* [MPMD-266](https://issues.apache.org/jira/browse/MPMD-266) - Aggregate report in multi-module projects doesn't use correct auxclasspath
* [MPMD-268](https://issues.apache.org/jira/browse/MPMD-268) - Missing warnings about deprecated rules

### 🚀 New features and improvements
* [MPMD-270](https://issues.apache.org/jira/browse/MPMD-270) - JDK 11 compatibility
* [MPMD-272](https://issues.apache.org/jira/browse/MPMD-272) - Support ignoreAnnotations options for CPD

### 📦 Dependency updates
* [MPMD-271](https://issues.apache.org/jira/browse/MPMD-271) - Upgrade pmd 6.8.0

## Version 3.10.0

**Release Date:** 2018-05-29

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.10.0](https://issues.apache.org/jira/projects/MPMD/versions/12342689)

### 🐛 Bug Fixes
* [MPMD-253](https://issues.apache.org/jira/browse/MPMD-253) - PMD links to java Xref fail in aggregated report
* [MPMD-257](https://issues.apache.org/jira/browse/MPMD-257) - Allow to disable analysisCache completely, avoid warnings
* [MPMD-258](https://issues.apache.org/jira/browse/MPMD-258) - PMD output multiplies with every module in multi module projects
* [MPMD-259](https://issues.apache.org/jira/browse/MPMD-259) - FileNotFoundException with analysisCache=true, includeTests=true and no test classes

### 🚀 New features and improvements
* [MPMD-256](https://issues.apache.org/jira/browse/MPMD-256) - Add maxAllowedViolations property for PMD
* [MPMD-264](https://issues.apache.org/jira/browse/MPMD-264) - Add rule priority to HTML report

### 📝 Documentation updates
* [MPMD-263](https://issues.apache.org/jira/browse/MPMD-263) - Add documentation information for GitHub

### 📦 Dependency updates
* [MPMD-252](https://issues.apache.org/jira/browse/MPMD-252) - Upgrade parent to 31
* [MPMD-261](https://issues.apache.org/jira/browse/MPMD-261) - Upgrade to PMD 6.4.0
* [MPMD-262](https://issues.apache.org/jira/browse/MPMD-262) - Upgrade maven-surefire/failsafe-plugin 2.21.0

## Version 3.9.0

**Release Date:** 2018-01-21

**JIRA:** [Release Notes - Maven PMD Plugin - Version 3.9.0](https://issues.apache.org/jira/projects/MPMD/versions/12340516)

### 💥 Breaking changes
* [MPMD-240](https://issues.apache.org/jira/browse/MPMD-240) - Migrate plugin to Maven 3.0

### 🐛 Bug Fixes
* [MPMD-244](https://issues.apache.org/jira/browse/MPMD-244) - Maven PMD plugin fails but no reason is given for the failure
* [MPMD-245](https://issues.apache.org/jira/browse/MPMD-245) - targetJdk property should use maven.compiler.source by default
* [MPMD-248](https://issues.apache.org/jira/browse/MPMD-248) - cpd-check goal leaks file handle
* [MPMD-249](https://issues.apache.org/jira/browse/MPMD-249) - The plugin documentation point to old (deprecated) rulesets
* [MPMD-251](https://issues.apache.org/jira/browse/MPMD-251) - Invalid report XML is generated with includeXmlInSite=true

### 🚀 New features and improvements
* [MPMD-246](https://issues.apache.org/jira/browse/MPMD-246) - Output details of processing errors

### 📝 Documentation updates
* [MPMD-239](https://issues.apache.org/jira/browse/MPMD-239) - Add documentation about upgrading PMD version at runtime

### 🔧 Build
* [MPMD-235](https://issues.apache.org/jira/browse/MPMD-235) - Javadoc errors when building with java8

### 📦 Dependency updates
* [MPMD-247](https://issues.apache.org/jira/browse/MPMD-247) - Upgrade to PMD 6.0.1
