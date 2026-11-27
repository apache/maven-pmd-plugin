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

// MPMD-656: before the fix, this build failed with a NullPointerException in at least one module because
// the modules' non-forked PMD runs raced on PMD's shared global TimeTracker under the parallel (-T) build.
for ( int i = 1; i <= 4; i++ )
{
    File benchmarkFile = new File( basedir, "mod-" + i + "/target/pmd-benchmark.txt" )
    assert benchmarkFile.isFile() : "Missing benchmark report for mod-" + i
    assert benchmarkFile.length() > 0 : "Empty benchmark report for mod-" + i
}
