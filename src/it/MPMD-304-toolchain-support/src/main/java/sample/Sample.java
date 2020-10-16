package sample;

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
import java.util.function.Function;

// this class trigger PMD rule category/java/codestyle.xml/ExtendsObject
public class Sample extends Object
{
    public static void main( String ... args )
    {
        new Sample();
    }

    public Sample()
    {
        // this triggers category/java/multithreading.xml/DontCallThreadRun
        // it only works, if the auxclass path could be loaded,
        // by using the correct jdk toolchain
        new Name( "foo" ).run();
        Name name = getName( new Name( "World" ) );
        Function<Name, String> greeter = (var n) -> "Hello " + n;
        System.out.println( greeter.apply( name ) );
    }

    private Name getName( Name name )
    {
        return new Name( name.toString() + "!" );
    }
}
