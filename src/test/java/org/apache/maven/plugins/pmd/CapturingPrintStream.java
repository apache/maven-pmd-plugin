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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintStream;

import org.slf4j.impl.MavenSlf4jSimpleFriend;

/**
 * Captures log output from simple slf4j for asserting in unit tests.
 */
class CapturingPrintStream extends PrintStream {
    private final boolean quiet;
    private StringBuilder buffer = new StringBuilder();

    private CapturingPrintStream( boolean quiet ) {
        super( System.out, true );
        this.quiet = quiet;
    }

    @Override
    public void println( String x )
    {
        if ( !quiet )
        {
            super.println( x );
        }
        buffer.append( x ).append( System.lineSeparator() );
    }

    public static void init( boolean quiet )
    {
        CapturingPrintStream capture = get();
        if ( capture != null )
        {
            capture.buffer.setLength( 0 );
        }
        else
        {
            capture = new CapturingPrintStream( quiet );
            System.setOut( capture );
            MavenSlf4jSimpleFriend.init();
        }
    }

    public static CapturingPrintStream get()
    {
        if ( System.out instanceof CapturingPrintStream )
        {
            return (CapturingPrintStream) System.out;
        }
        return null;
    }

    public static String getOutput()
    {
        CapturingPrintStream stream = get();
        if ( stream != null )
        {
            stream.flush();
            return stream.buffer.toString();
        }
        return "";
    }
}