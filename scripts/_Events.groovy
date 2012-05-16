import groovy.xml.StreamingMarkupBuilder
import org.codehaus.groovy.grails.commons.ConfigurationHolder

eventCompileEnd = {
    if (!isPluginProject) {
        grailsConsole.echo 'checking sitemesh decorators for events-bus'
        buildConfiguration(basedir)
    }
}


def buildConfiguration(basedir) {
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
    def file = new File("$basedir/web-app/WEB-INF/sitemesh.xml")
    def doc = new XmlSlurper().parse(file)
    if (!doc.excludes.find { it.@file == '/WEB-INF/atmosphere-decorators.xml' }.size()) {
        doc.appendNode({ excludes(file: '/WEB-INF/atmosphere-decorators.xml') })
        // Save the XML document with pretty print
        def xml = new StreamingMarkupBuilder().bind {
            mkp.yield(doc)
        }
        def node = new XmlParser().parseText(xml.toString())
        file.withWriter {
            new XmlNodePrinter(new PrintWriter(it)).print(node)
        }
    }
}
