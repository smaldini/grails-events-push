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

import groovy.xml.StreamingMarkupBuilder
import org.codehaus.groovy.grails.commons.ConfigurationHolder

eventCompileEnd = {
    if (!isPluginProject) {
        buildConfiguration(basedir)
    }
}


def buildConfiguration(basedir) {
    def sitemeshXml = new File("$basedir/web-app/WEB-INF/sitemesh.xml")

    if (!sitemeshXml.exists())
        return

    def config = ConfigurationHolder.config?.events?.push
    def urlPattern = config?.servlet?.urlPattern ?: '/g-eventsbus/*'
    // Write the atmosphere-decorators.xml file in WEB-INF
    def decoratorsDotXml = """\
<decorators>
    <excludes>
        <pattern>$urlPattern</pattern>
    </excludes>
</decorators>"""



    new File("$basedir/web-app/WEB-INF/atmosphere-decorators.xml").write decoratorsDotXml

    // Modify if necessary the sitemesh.xml file that is in WEB-INF?
    def doc = new XmlSlurper().parse(sitemeshXml)
    if (!doc.excludes.find { it.@file == '/WEB-INF/atmosphere-decorators.xml' }.size()) {
        doc.appendNode({ excludes(file: '/WEB-INF/atmosphere-decorators.xml') })
        // Save the XML document with pretty print
        def xml = new StreamingMarkupBuilder().bind {
            mkp.yield(doc)
        }
        def node = new XmlParser().parseText(xml.toString())
        sitemeshXml.withWriter {
            new XmlNodePrinter(new PrintWriter(it)).print(node)
        }
    }
}
