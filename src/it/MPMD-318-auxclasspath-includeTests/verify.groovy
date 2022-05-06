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

def auxclasspathLog = ''
for ( String line : buildLog.readLines() ) {
    if ( line.contains( 'aux classpath:' ) ) {
      auxclasspathLog += line + '\n';
    }
}

// convert windows path names
auxclasspathLog = auxclasspathLog.replaceAll('\\\\', '/')

assert 1 == auxclasspathLog.count( 'Using aux classpath:' )
assert 1 == auxclasspathLog.count( 'Using aggregated aux classpath:' )

assert 2 == auxclasspathLog.count( 'module-a/target/test-classes' )
assert 2 == auxclasspathLog.count( 'module-a/target/classes' )

// compile
assert 2 == auxclasspathLog.count( 'org/apache/commons/commons-lang3/3.8.1/commons-lang3-3.8.1.jar' )
// provided
assert 2 == auxclasspathLog.count( 'javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar' )
// test
assert 2 == auxclasspathLog.count( 'commons-io/commons-io/2.7/commons-io-2.7.jar' )
