import org.grails.plugin.platform.events.push.EventsPushScopes

includes = ['default']

doWithReactor = {

	reactor(EventsPushScopes.FROM_BROWSERS){ }
}