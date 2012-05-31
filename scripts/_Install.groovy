// Write the context.xml file in META-INF and WEB-INF
def contextDotXml = """\
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Context>
    <Loader delegate=\"true\"/>
</Context>"""
def metaInf = new File("$basedir/web-app/META-INF/")
def webInf = new File("$basedir/web-app/WEB-INF/")

if(!metaInf.exists())
    metaInf.mkdirs()

if(!webInf.exists())
    webInf.mkdirs()

new File(metaInf, "context.xml").write contextDotXml
new File(webInf, "context.xml").write contextDotXml