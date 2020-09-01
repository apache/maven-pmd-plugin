
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

assert buildLog.text.contains( "[INFO] CPD Failure: Found 7 lines of duplicated code at locations" )
assert buildLog.text.contains( "[DEBUG] PMD failureCount: 1, warningCount: 0" )

File cpdXml = new File( basedir, 'target/cpd.xml' )
assert cpdXml.exists()

// no duplication for the license header - if this is reported, then CPD uses the wrong language/tokenizer
assert !cpdXml.text.contains( '<duplication lines="20" tokens="148">' )
assert !cpdXml.text.contains( 'line="1"' )

// the only valid duplication
assert cpdXml.text.contains( '<duplication lines="7" tokens="26">' )
assert cpdXml.text.contains( 'line="20"' )
