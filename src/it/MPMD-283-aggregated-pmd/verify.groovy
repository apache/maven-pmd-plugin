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

def paths = [
    "target/site/pmd.html",
    "target/site/cpd.html",
    "target/pmd.xml",
    "target/cpd.xml",
]

// aggregated reports must exist
for ( String path : paths )
{
    File file = new File( basedir, path )
    System.out.println( "Checking for existence of " + file )
    if ( !file.isFile() )
    {
        throw new RuntimeException( "Missing: " + file )
    }
}

// double check violations: these violations only appear, if type resolution didn't work
// in case the modules have not been compiled before
File pmdXml = new File( basedir, "target/pmd.xml" )
assert !pmdXml.text.contains( "Avoid unused private methods such as 'doSomething(IModuleA)'." )
assert !pmdXml.text.contains( "Avoid unused private methods such as 'aPrivateMethod(FieldElement)'." )

File pmdHtml = new File( basedir, "target/site/pmd.html" )
assert !pmdHtml.text.contains( "Avoid unused private methods such as 'doSomething(IModuleA)'." )
assert !pmdHtml.text.contains( "Avoid unused private methods such as 'aPrivateMethod(FieldElement)'." )

// no individual module reports
def modules = [ "module-a", "module-b" ]
for ( String module : modules )
{
  for ( String path : paths )
  {
    File file = new File( basedir, "${module}/${path}" )
    System.out.println( "Checking for absence of " + file )
    if ( file.exists() )
    {
        throw new RuntimeException( "Banned: " + file )
    }
  }
}
