import org.grails.plugin.platform.events.push.EventsPushScopes
import reactor.event.dispatch.SynchronousDispatcher

includes = ['default']

doWithReactor = {

	reactor(EventsPushScopes.FROM_BROWSERS){
		dispatcher = new SynchronousDispatcher()
	}
}