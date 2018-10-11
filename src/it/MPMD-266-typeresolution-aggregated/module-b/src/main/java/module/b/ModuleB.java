package module.b;

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

import module.a.IModuleA;
import module.a.ModuleA;
import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.FieldElement;

public class ModuleB
{
    public static void main( String[] args )
    {
        ModuleA m = new ModuleA();
        doSomething( m );
    }

    // this method will be detected as being unsued,
    // if typeresolution is not setup correctly: module a needs
    // to be on PMD's auxclasspath, so that PMD knows, that ModuleA
    // implements IModuleA
    private static void doSomething( IModuleA module )
    {
        System.out.println( module );
    }

    public static void aPublicMethod()
    {
        Complex u = new Complex(1, 1);
        aPrivateMethod( u );
    }

    private static void aPrivateMethod( FieldElement<Complex> u )
    {
        System.out.println( "aPrivateMethod: " + u );
    }
}