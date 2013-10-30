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
import org.grails.plugin.platform.events.push.EventsPushHandler
import org.grails.plugin.platform.events.push.GrailsMeteorServlet
import org.grails.plugin.platform.events.push.GrailsWebsocketProtocol
import org.grails.plugins.events.reactor.configuration.EventsArtefactHandler
import org.springframework.util.ClassUtils

class EventsPushGrailsPlugin {
	// the plugin version
	def version = "1.0.0.BUILD-SNAPSHOT"
	// the version or versions of Grails the plugin is designed for
	def grailsVersion = "2.2 > *"
	// the other plugins this plugin depends on
	def loadAfter = ['events']
	// resources that are excluded from plugin packaging
	def pluginExcludes = [
			"grails-app/views/error.gsp",
			"grails-app/views/index.gsp",
			"grails-app/conf/Test*.groovy",
			"grails-app/domain/**/Test*.groovy",
			"grails-app/services/**/Test*.groovy",
			"web-app/css/**",
			"web-app/images/**",
			"web-app/js/application.js",
			"web-app/WEB-INF/**"
	]

	def observe = ['events']

	def title = "Events Push Plugin" // Headline display name of the plugin
	def author = "Stephane Maldini"
	def authorEmail = "smaldini@gopivotal.com"
	def description = '''\
This is a client-side event bus based on the portable push library [Atmosphere|https://github.com/Atmosphere/atmosphere]\
that propagates events from the server-side event bus provided by the [Events Plugin |http://grails
.org/plugin/events]\
to the browser. It allows your client Javascript code to both send events and listen for them.

For security, events-push is a white-list broadcaster so that you can control exactly which events are propagated from\
the server to the browser.
'''

	// URL to the plugin's documentation
	def documentation = "https://github.com/smaldini/grails-events-push/blob/master/README.md"

	// Extra (optional) plugin metadata

	// License: one of 'APACHE', 'GPL2', 'GPL3'
	def license = "APACHE"

	// Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

	// Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

	// Location of the plugin's issue tracker.
	def issueManagement = [system: "GITHUB", url: "https://github.com/smaldini/grails-events-push/issues"]

	// Online location of the plugin's browseable source code.
	def scm = [url: "https://github.com/smaldini/grails-events-push"]

	def doWithSpring = {
		eventsPushHandler(EventsPushHandler){ bean->
			grailsEvents = ref('instanceEventsApi')
			grailsApplication = ref('grailsApplication')
			bean.lazyInit = true
		}
	}

	def doWithWebDescriptor = { xml ->
		def servlets = xml.'servlet'
		def config = application.config?.grails?.events?.push

		servlets[servlets.size() - 1] + {
			'servlet' {
				'description'('MeteorServlet')
				'servlet-name'('MeteorServlet')
				'servlet-class'(GrailsMeteorServlet.name)
				config?.servlet?.initParams?.each { initParam ->
					if (initParam.key && initParam.value) {
						'init-param' {
							'param-name'(initParam.key)
							'param-value'(initParam.value)
						}
					}
				}

				if (!config?.servlet?.initParams?."org.atmosphere.cpr.broadcaster.shareableThreadPool")
					'init-param' {
						'param-name'('org.atmosphere.cpr.broadcaster.shareableThreadPool')
						'param-value'(true)
					}

				if (!config?.servlet?.initParams?."org.atmosphere.cpr.broadcaster.maxProcessingThreads")
					'init-param' {
						'param-name'('org.atmosphere.cpr.broadcaster.maxProcessingThreads')
						'param-value'(20)
					}

				if (!config?.servlet?.initParams?."org.atmosphere.cpr.broadcaster.maxAsyncWriteThreads")
					'init-param' {
						'param-name'('org.atmosphere.cpr.broadcaster.maxAsyncWriteThreads')
						'param-value'(20)
					}

				if (!config?.servlet?.initParams?."org.atmosphere.websocket.WebSocketProtocol")
					'init-param' {
						'param-name'('org.atmosphere.websocket.WebSocketProtocol')
						'param-value'(GrailsWebsocketProtocol.name)
					}

				/*if (!config?.servlet?.initParams?."org.atmosphere.websocket.binaryWrite")
					'init-param' {
						'param-name'('org.atmosphere.websocket.binaryWrite')
						'param-value'(true)
					}
*/
				'load-on-startup'('0')

				if (ClassUtils.isPresent("javax.servlet.AsyncContext", Thread.currentThread().getContextClassLoader())) {
					'async-supported'(true)
				}
			}
		}

		def mappings = xml.'servlet-mapping'
		mappings[mappings.size() - 1] + {
			'servlet-mapping' {
				'servlet-name'('MeteorServlet')
				'url-pattern'(config?.servlet?.urlPattern ?: '/g-eventsbus/*')
			}
		}
	}

}
