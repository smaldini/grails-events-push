import org.grails.plugin.platform.events.push.EventsPushScopes
import static reactor.event.selector.Selectors.*

includes = ['push']

doWithReactor = {

	reactor('grailsReactor'){

		ext 'gorm', true
		ext 'browser', [
				'test' : true,
				'afterInsert' : [
						browserFilter:{m,r-> true }
				],
				(R("sampleBro-.*")) : true,
		]
	}

	reactor(EventsPushScopes.FROM_BROWSERS){
		ext 'browser', [R("sampleBro-.*")]
	}
}