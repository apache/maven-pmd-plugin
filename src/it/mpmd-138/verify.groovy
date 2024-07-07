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

import groovy.xml.XmlSlurper

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

// Module 1
File pmdXml = new File( basedir, "mod-1/target/pmd.xml" )
assert pmdXml.exists()

def pmd = new XmlSlurper().parse( pmdXml )
def version = pmd.@version

assert buildLog.getText().contains('[WARNING] PMD Failure: test.MyClass:27 Rule:UnnecessarySemicolon Priority:3 Unnecessary semicolon')
assert buildLog.getText().contains('[WARNING] PMD Failure: test.MyClass:28 Rule:UnnecessaryReturn Priority:3 Unnecessary return statement')
assert buildLog.getText().contains('[WARNING] PMD ' + version + ' has found 2 violations. For more details see:')

// Module 2
pmdXml = new File( basedir, "mod-2/target/pmd.xml" )
assert pmdXml.exists()

pmd = new XmlSlurper().parse( pmdXml )
version = pmd.@version

assert buildLog.getText().contains('[WARNING] PMD Failure: test.MyClass:24 Rule:UnusedPrivateField Priority:3 Avoid unused private fields such as \'x\'')
assert buildLog.getText().contains('[WARNING] PMD ' + version + ' has found 1 violation. For more details see:')

// Module 3
File cpdXml = new File( basedir, "mod-3/target/cpd.xml" )
assert cpdXml.exists()

def cpd = new XmlSlurper().parse( cpdXml )
def pmdVersion = cpd.@pmdVersion

assert buildLog.getText().contains('[WARNING] CPD Failure: Found 37 lines of duplicated code at locations:')
assert buildLog.getText().contains('[WARNING] CPD ' + pmdVersion + ' has found 1 duplication. For more details see:')

// Module 4
cpdXml = new File( basedir, "mod-4/target/cpd.xml" )
assert cpdXml.exists()

cpd = new XmlSlurper().parse( cpdXml )
pmdVersion = cpd.@pmdVersion

assert buildLog.getText().contains('[WARNING] CPD Failure: Found 35 lines of duplicated code at locations:')
assert buildLog.getText().contains('[WARNING] CPD Failure: Found 34 lines of duplicated code at locations:')
assert buildLog.getText().contains('[WARNING] CPD ' + pmdVersion + ' has found 2 duplications. For more details see:')
