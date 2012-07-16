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

class EventsPushGrailsPlugin {
    // the plugin version
    def version = "1.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def loadAfter = ['platformCore']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/views/index.gsp",
            "grails-app/conf/Test*.groovy",
            "grails-app/domain/**/Test*.groovy",
            "grails-app/services/**/Test*.groovy"
    ]

    def observe = ['platformCore']

    def title = "Events Push Plugin" // Headline display name of the plugin
    def author = "Stephane Maldini"
    def authorEmail = "stephane.maldini@gmail.com"
    def description = '''\
This is a client-side event bus based on the portable push library [Atmosphere|https://github.com/Atmosphere/atmosphere]\
that propagates events from the server-side event bus provided by the [Platform Core|http://grails.org/plugin/platform-core]\
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
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/smaldini/grails-events-push"]

    def doWithWebDescriptor = { xml ->
        def servlets = xml.'servlet'
        def config = application.config?.events?.push

        servlets[servlets.size() - 1] + {
            'servlet' {
                'description'('MeteorServlet')
                'servlet-name'('MeteorServlet')
                'servlet-class'(GrailsMeteorServlet.name)
                if (!config?.servlet?.initParams?.'org.atmosphere.useWebSocket') {
                    'init-param' {
                        'param-name'('org.atmosphere.useWebSocket')
                        'param-value'(true)
                    }
                }
                config?.servlet?.initParams.each { initParam ->
                    'init-param' {
                        'param-name'(initParam.key)
                        'param-value'(initParam.value)
                    }
                }
                'load-on-startup'('0')
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

    def onChange = { event ->
        if (application.isArtefactOfType('Events', event.source)) {
            EventsPushHandler.registerTopics(event.ctx.grailsEventsRegistry, event.ctx.grailsEvents)
        }
    }
}
