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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PmdReportNumberOfThreadsTest {

    /**
     * Verify where no specific request for thread configuration is requested that the control is deferred to pmd itself.
     */
    @Test
    public void testDefaultConfiguration() {
        assertNull(PmdReport.numThreadsConverter(null, 8));
    }

    /**
     * Verify that a request for a fixed integer number of threads is honoured
     */
    @Test
    public void testValidFixedThreadConfiguration() {
        assertEquals(1, PmdReport.numThreadsConverter("1", 2));
        assertEquals(3, PmdReport.numThreadsConverter("3", 8));
    }

    /**
     * Verify that a request for an invalid fixed number of threads is rejected
     */
    @Test
    public void testInvalidFixedThreadConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("0.9", 4));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("1.0", 4));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("1.5", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("-1", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("1,0", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("1_0", 8));
    }

    /**
     * Verify that a request for a valid number of threads related to the CPUs available is honoured
     */
    @Test
    public void testValidMultiplierThreadConfiguration() {
        assertEquals(0, PmdReport.numThreadsConverter("0.1C", 2));
        assertEquals(0, PmdReport.numThreadsConverter("0.1C", 8));
        assertEquals(1, PmdReport.numThreadsConverter("0.1C", 12));
        assertEquals(1, PmdReport.numThreadsConverter("0.1C", 16));
        assertEquals(2, PmdReport.numThreadsConverter("0.1C", 24));
        assertEquals(8, PmdReport.numThreadsConverter("1C", 8));
        assertEquals(12, PmdReport.numThreadsConverter("1.5C", 8));
        assertEquals(16, PmdReport.numThreadsConverter("2C", 8));
    }

    /**
     * Verify that a request for a invalid number of threads related to the CPUs available is rejected
     */
    @Test
    public void testInvalidMultiplierThreadConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("0C", 4));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("-1.0C", 4));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("-1C", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("C", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("0..5C", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("0,5C", 8));
        assertThrows(IllegalArgumentException.class, () -> PmdReport.numThreadsConverter("0_5C", 8));
    }
}
