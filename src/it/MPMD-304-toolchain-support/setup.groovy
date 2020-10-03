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

File toolchains = new File( basedir, 'toolchains.xml' )
String placeholder = '@jdk.home@'
String replacement = System.getProperty( 'java.home' )
// extra escaping of backslashes in the path for Windows
replacement = replacement.replaceAll("\\\\", "\\\\\\\\")
toolchains.text = toolchains.text.replaceAll( placeholder, replacement )
System.out.println( "Replaced '${placeholder}' with '${replacement}' in '${toolchains.absolutePath}'." )

//return true

// check user toolchains.xml
File userToolchains = new File( System.getProperty( 'user.home' ), '.m2/toolchains.xml' )
throw new RuntimeException( "java.home=${replacement}\nUserToolchains (${userToolchains.absolutePath}):\n${userToolchains.text}" )
