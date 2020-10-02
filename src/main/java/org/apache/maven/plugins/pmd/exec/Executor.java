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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import org.apache.maven.shared.utils.logging.MessageUtils;
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

    protected void setupPmdLogging( boolean showPmdLog, boolean colorizedLog, String logLevel )
    {
        MessageUtils.setColorEnabled( colorizedLog );

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
            LOG.info( "slf4jBridge is already added" );
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

}
