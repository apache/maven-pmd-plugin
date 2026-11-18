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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TargetJdkResolutionTest {

    @Test
    void explicitTargetJdkWins() {
        assertEquals("11", PmdReport.resolveTargetJdk("11", "17", "1.8"));
    }

    @Test
    void releaseIsUsedBeforeSource() {
        assertEquals("17", PmdReport.resolveTargetJdk(null, "17", "1.8"));
    }

    @Test
    void sourceIsUsedWithoutRelease() {
        assertEquals("1.8", PmdReport.resolveTargetJdk(null, null, "1.8"));
        assertEquals("1.8", PmdReport.resolveTargetJdk(" ", "", "1.8"));
    }

    @Test
    void nothingSetGivesNull() {
        assertNull(PmdReport.resolveTargetJdk(null, null, null));
    }
}
