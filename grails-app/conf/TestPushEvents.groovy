import org.groovy.grails.platform.push.TestDomain
/* Copyright 2011-2012 the original author or authors:
*
*    Marc Palmer (marc@grailsrocks.com)
*    Stéphane Maldini (stephane.maldini@gmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

events = {
    "sampleBro" browser: true, //Will allow client to register for events push
            scope: "*" //Will allow both server and client to send events on this topic due to scope:'*'

    /*
    Will allow client to register for events push, every GORM afterInsert events will be
    propagated
    */
    "afterInsert" browser: true, filter:TestDomain
}