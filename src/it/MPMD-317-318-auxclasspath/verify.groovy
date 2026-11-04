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

// build.log holds all four invocations, in order
def auxclasspathLines = []
for ( String line : buildLog.readLines() ) {
    if ( line.contains( 'aux classpath:' ) ) {
      // convert windows path names
      auxclasspathLines << line.replaceAll( '\\\\', '/' )
    }
}

assert 4 == auxclasspathLines.size()
def aux = auxclasspathLines[0]
def auxAggregated = auxclasspathLines[1]
def auxIncludeTests = auxclasspathLines[2]
def auxAggregatedIncludeTests = auxclasspathLines[3]

assert aux.contains( '[DEBUG] Using aux classpath:' )
assert auxAggregated.contains( '[DEBUG] Using aggregated aux classpath:' )
assert auxIncludeTests.contains( '[DEBUG] Using aux classpath:' )
assert auxAggregatedIncludeTests.contains( '[DEBUG] Using aggregated aux classpath:' )

for ( String line : auxclasspathLines ) {
    assert line.contains( 'module-a/target/classes' )
    // compile
    assert line.contains( 'org/apache/commons/commons-lang3/3.18.0/commons-lang3-3.18.0.jar' )
    // provided
    assert line.contains( 'javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar' )
}

// includeTests is off
for ( String line : [ aux, auxAggregated ] ) {
    assert !line.contains( 'module-a/target/test-classes' )
    assert !line.contains( 'commons-io/commons-io/2.14.0/commons-io-2.14.0.jar' )
}

// includeTests is on
for ( String line : [ auxIncludeTests, auxAggregatedIncludeTests ] ) {
    assert line.contains( 'module-a/target/test-classes' )
    // test
    assert line.contains( 'commons-io/commons-io/2.14.0/commons-io-2.14.0.jar' )
}
