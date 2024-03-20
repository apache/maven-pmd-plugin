
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

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

assert buildLog.text.contains( '[INFO] Toolchain in maven-pmd-plugin: JDK[' )
assert buildLog.text.contains( '[INFO] PMD Failure: sample.Sample:24 Rule:ExtendsObject' )
assert buildLog.text.contains( '[INFO] PMD Failure: sample.Sample:36 Rule:DontCallThreadRun' )
assert buildLog.text.contains( '[INFO] You have 2 PMD violations.' )
assert buildLog.text.contains( '[INFO] You have 1 CPD duplication' )

File pmdReport = new File( basedir, 'target/pmd.xml' )
assert pmdReport.exists()
assert pmdReport.text.contains( '<violation beginline="24" endline="24" begincolumn="29" endcolumn="34" rule="ExtendsObject"' )
assert pmdReport.text.contains( '<violation beginline="36" endline="36" begincolumn="9" endcolumn="31" rule="DontCallThreadRun"' )

File pmdHtmlReport = new File( basedir, 'target/reports/pmd.html' )
assert pmdHtmlReport.exists()
assert pmdHtmlReport.text.contains( 'Sample.java' )
assert pmdHtmlReport.text.contains( 'ExtendsObject' )
assert pmdHtmlReport.text.contains( 'DontCallThreadRun' )

File cpdReport = new File( basedir, 'target/cpd.xml' )
assert cpdReport.exists()
assert cpdReport.text.contains( 'Name.java' )
assert cpdReport.text.contains( 'Name2.java' )

File cpdHtmlReport = new File( basedir, 'target/reports/cpd.html' )
assert cpdHtmlReport.exists()
assert cpdHtmlReport.text.contains( 'Name.java' )
assert cpdHtmlReport.text.contains( 'Name2.java' )
