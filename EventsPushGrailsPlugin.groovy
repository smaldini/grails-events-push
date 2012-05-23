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

class EventsPushGrailsPlugin {
    // the plugin version
    def version = "1.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/views/index.gsp",
            "grails-app/conf/Test*.groovy",
            "grails-app/services/**/Test*.groovy"
    ]

    def observe = ['events']

    def title = "Events Push Plugin" // Headline display name of the plugin
    def author = "Stephane Maldini"
    def authorEmail = "stephane.maldini@gmail.com"
    def description = '''\
Events-push is a client-side events bus based on the portable push library Atmosphere and Grails platform-core plugin for events
propagation/listening. It simply allows your client to listen to server-side events and push data. It uses WebSockets by default
and failbacks to Comet method if required (server not compliant, browser too old...).
Events-push is a white-list broadcaster (triggered scope is 'browser', where your default listener scope with platform-core plugin is 'app'). You
will need to define which events that can be propagated to server-side by using Events DSL to override 'browser' scope. Ie:

MyEvents.groovy >
events = {
    'saveTodo' scope:'browser' //change 'saveTodo' listeners scope for browser, hence receiving client data.
}

someView.html >
grailsEvents.push('saveTodo', data);
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/events-push"

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
                'servlet-class'('org.atmosphere.cpr.MeteorServlet')
                if (!config?.servlet?.initParams?.'org.atmosphere.servlet') {
                    'init-param' {
                        'param-name'('org.atmosphere.servlet')
                        'param-value'(EventsPushHandler.name)
                    }
                }
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
        EventsPushHandler.registerTopics(event.ctx.grailsEventsRegistry, event.ctx.grailsEvents)
    }
}
