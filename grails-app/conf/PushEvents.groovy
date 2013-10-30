import org.grails.plugin.platform.events.push.EventsPushConstants
import reactor.event.dispatch.SynchronousDispatcher

includes = ['default']

doWithReactor = {

	reactor(EventsPushConstants.FROM_BROWSERS){
	}
}