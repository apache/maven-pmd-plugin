package org.apache.maven.plugins.pmd;

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

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Creates a report for PMD's Copy/Paste Detector (CPD) tool in an <b>aggregator</b> project.
 * It can also generate a cpd results file in any of these formats: xml, csv or txt.
 *
 * <p>See <a href="https://pmd.github.io/latest/pmd_userdocs_cpd.html">Finding duplicated code</a>
 * for more details.
 *
 * @since 3.15.0
 */
@Mojo( name = "aggregate-cpd", aggregator = true, threadSafe = true )
public class AggregatorCpdReport extends CpdReport
{
    @Override
    protected boolean isAggregator()
    {
        return true;
    }
}
