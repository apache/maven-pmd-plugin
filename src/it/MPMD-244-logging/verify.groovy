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
assert buildLog.text.contains( "PMD processing errors" )
assert buildLog.text.contains( "ParseException: Parse exception" )
assert buildLog.text.contains( "at line 24, column 5: Encountered" )

String enabledPath = new File( basedir, 'logging-enabled/src/main/java/BrokenFile.java' ).getCanonicalPath()

// logging enabled: the pmd exception is still output only once: through the processing error reporting (since MPMD-246) - PMD 7 doesn't log the processing error additionally
assert 1 == buildLog.text.count( "${enabledPath}: ParseException: Parse exception in" )

// build.log contains the logging from the two PMD executions
// only one execution has logging enabled, so we expect only one log output
assert 1 == buildLog.text.count( "[DEBUG] Rules loaded from" )
// with --debug switch or -X the logging is always enabled and can't be disabled , because PMD 7 switched to slf4j
