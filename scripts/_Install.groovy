// Write the context.xml file in META-INF and WEB-INF
def contextDotXml = """\
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Context>
    <Loader delegate=\"true\"/>
</Context>"""
new File("$basedir/web-app/META-INF/context.xml").write contextDotXml
new File("$basedir/web-app/WEB-INF/context.xml").write contextDotXml