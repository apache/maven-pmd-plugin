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

File testToolchains = new File( basedir, 'toolchains.xml' )

File userToolchains = new File( System.getProperty( 'user.home' ), '.m2/toolchains.xml' )
if ( userToolchains.exists() )
{
    System.out.println( "INFO: Copying ${userToolchains.absolutePath} to ${testToolchains.absolutePath}" )
    testToolchains.text = userToolchains.text
}
else
{
    System.out.println( "WARNING: File ${userToolchains.absolutePath} not found" )
    if ( System.getProperty( 'os.name' ).startsWith( 'Windows' ) )
    {
        String jdk11Windows = 'f:\\jenkins\\tools\\java\\latest11'
        File windowsToolchains = new File( basedir, 'toolchains.windows.xml' )
        System.out.println( "INFO: Creating ${testToolchains.absolutePath} with jdk:11:oracle=${jdk11Windows}" )

        String placeholder = '@jdk.home@'
        String replacement = jdk11Windows
        // extra escaping of backslashes in the path for Windows
        replacement = replacement.replaceAll("\\\\", "\\\\\\\\")
        testToolchains.text = windowsToolchains.text.replaceAll( placeholder, replacement )
        System.out.println( "Replaced '${placeholder}' with '${replacement}' in '${testToolchains.absolutePath}'." )
    }
}

if ( testToolchains.exists() )
{
    def toolchains = new XmlParser().parseText( testToolchains.text )
    def result = toolchains.children().find { toolchain ->
            toolchain.type.text() == 'jdk' &&
            toolchain.provides.version.text() == '11' &&
            toolchain.provides.vendor.text() == 'oracle'
    }
    if ( !result )
    {
        System.out.println( "WARNING: No jdk toolchain for 11:oracle found" )
        return false
    }

    System.out.println( "INFO: Found toolchain: ${result}" )
    return true
}

System.out.println( "WARNING: Skipping integration test due to missing toolchain" )
return false
