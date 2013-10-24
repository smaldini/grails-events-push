grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.dependency.resolver = "maven"
grails.tomcat.nio = true
//grails.project.war.file = "target/${appName}-${appVersion}.war"
//grails.plugin.location.'platformCore' = '../../platform-core'


grails.project.dependency.resolution = {
	// inherit Grails' default dependencies
	inherits("global") {
		excludes("xml-apis", "commons-digester")
	}

	log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()


		mavenLocal()
		mavenCentral()
		mavenRepo "http://repo.springsource.org/libs-milestone"
		mavenRepo "http://repo.springsource.org/libs-snapshot"
		mavenRepo "http://repo.grails.org/grails/libs-snapshots-local/"
		mavenRepo "https://oss.sonatype.org/content/repositories/snapshots"
	}
	dependencies {
		compile 'org.grails.plugins:events:1.0.0.BUILD-SNAPSHOT'
		compile 'org.atmosphere:atmosphere-runtime:2.1.0-SNAPSHOT'
	}

	plugins {
		runtime(":jquery:1.8.2", ":hibernate:3.6.10.2") {
			export = false
		}

		runtime(":resources:1.2")
		build(":tomcat:7.0.42",
				":release:3.0.0", ":rest-client-builder:1.0.3"
		) {
			export = false
		}
	}
}
