// Write the context.xml file in META-INF and WEB-INF
def contextDotXml = """\
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Loader delegate="true"/>
</Context>"""

def webAppDir = new File(grailsSettings.baseDir, "web-app")
def webInfDir = new File(webAppDir, "WEB-INF")
def metaInfDir = new File(webAppDir, "META-INF")

if (!webInfDir.exists())) {
    event "StatusUpdate", [ "Your project does not have a 'web-app/WEB-INF' directory. Perhaps it's corrupt?" ]
}

webInfDir.mkdirs()
metaInfDir.mkdirs()
new File(webInfDir, "context.xml").write contextDotXml
new File(metaInfDir, "context.xml").write contextDotXml