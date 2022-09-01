package org.apache.maven.plugins.pmd.exec;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

abstract class Executor
{
    private static final Logger LOG = LoggerFactory.getLogger( Executor.class );

    /**
     * This holds a strong reference in case we configured the logger to
     * redirect to slf4j. See {@link #showPmdLog}. Without a strong reference,
     * the logger might be garbage collected and the redirect to slf4j is gone.
     */
    private java.util.logging.Logger julLogger;

    protected void setupPmdLogging( boolean showPmdLog, String logLevel )
    {
        if ( !showPmdLog )
        {
            return;
        }

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger( "net.sourceforge.pmd" );

        boolean slf4jBridgeAlreadyAdded = false;
        for ( Handler handler : logger.getHandlers() )
        {
            if ( handler instanceof SLF4JBridgeHandler )
            {
                slf4jBridgeAlreadyAdded = true;
                break;
            }
        }

        if ( slf4jBridgeAlreadyAdded )
        {
            return;
        }

        SLF4JBridgeHandler handler = new SLF4JBridgeHandler();
        SimpleFormatter formatter = new SimpleFormatter();
        handler.setFormatter( formatter );
        logger.setUseParentHandlers( false );
        logger.addHandler( handler );
        handler.setLevel( Level.ALL );
        logger.setLevel( Level.ALL );
        julLogger = logger;
        julLogger.fine( "Configured jul-to-slf4j bridge for " + logger.getName() );
    }

    protected void setupLogLevel( String logLevel )
    {
        ILoggerFactory slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory
                .getConfiguration( slf4jLoggerFactory );
        if ( "debug".equals( logLevel ) )
        {
            slf4jConfiguration
                    .setRootLoggerLevel( Slf4jConfiguration.Level.DEBUG );
        }
        else if ( "info".equals( logLevel ) )
        {
            slf4jConfiguration
                    .setRootLoggerLevel( Slf4jConfiguration.Level.INFO );
        }
        else
        {
            slf4jConfiguration
                    .setRootLoggerLevel( Slf4jConfiguration.Level.ERROR );
        }
        slf4jConfiguration.activate();
    }

    protected static String buildClasspath()
    {
        StringBuilder classpath = new StringBuilder();

        // plugin classpath needs to come first
        ClassLoader pluginClassloader = Executor.class.getClassLoader();
        buildClasspath( classpath, pluginClassloader );

        ClassLoader coreClassloader = ConsoleLogger.class.getClassLoader();
        buildClasspath( classpath, coreClassloader );

        return classpath.toString();
    }

    static void buildClasspath( StringBuilder classpath, ClassLoader cl )
    {
        if ( cl instanceof URLClassLoader )
        {
            for ( URL url : ( (URLClassLoader) cl ).getURLs() )
            {
                if ( "file".equalsIgnoreCase( url.getProtocol() ) )
                {
                    try
                    {
                        String filename = URLDecoder.decode( url.getPath(), StandardCharsets.UTF_8.name() );
                        classpath.append( new File( filename ).getPath() ).append( File.pathSeparatorChar );
                    }
                    catch ( UnsupportedEncodingException e )
                    {
                        LOG.warn( "Ignoring " + url + " in classpath due to UnsupportedEncodingException", e );
                    }
                }
            }
        }
    }

    protected static class ProcessStreamHandler implements Runnable
    {
        private static final int BUFFER_SIZE = 8192;

        private final BufferedInputStream in;
        private final BufferedOutputStream out;

        public static void start( InputStream in, OutputStream out )
        {
            Thread t = new Thread( new ProcessStreamHandler( in, out ) );
            t.start();
        }

        private ProcessStreamHandler( InputStream in, OutputStream out )
        {
            this.in = new BufferedInputStream( in );
            this.out = new BufferedOutputStream( out );
        }

        @Override
        public void run()
        {
            byte[] buffer = new byte[BUFFER_SIZE];
            try
            {
                int count = in.read( buffer );
                while ( count != -1 )
                {
                    out.write( buffer, 0, count );
                    out.flush();
                    count = in.read( buffer );
                }
                out.flush();
            }
            catch ( IOException e )
            {
                LOG.error( e.getMessage(), e );
            }
        }
    }
}
