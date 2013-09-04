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
package org.groovy.grails.platform.push

import groovy.transform.CompileStatic
import reactor.spring.annotation.Selector


/**
 * @file
 * @author Stephane Maldini <smaldini@doc4web.com>
 * @version 1.0
 * @date 21/05/12
 * @section DESCRIPTION
 *
 * [Does stuff]
 */
@CompileStatic
class TestService {

	//Listen for sampleBro events, the TestEvents.groovy DSL will configure this topic to observe clients by using scope : 'browser'
	@Selector(value = 'sampleBro-1', reactor = 'browser')
	def sampleBro(test) {
		def ts=new TestDomain(name:'test')

		ts.save() //This will trigger the GORM event 'afterInsert' where we have allowed for client listeners in TestEvents.groovy.
		//any browsers using grailsEvents.on('afterInsert', function(data){...}); will receive a JSON from TestDomain
	}

    @Selector(reactor ='browser')
    def onDisconnect(event){
        println 'disconnect'
    }
}
