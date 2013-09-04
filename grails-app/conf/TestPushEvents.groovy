import org.grails.plugin.platform.events.push.SharedConstants
import org.groovy.grails.platform.push.TestDomain
import static reactor.event.selector.Selectors.*

includes = ['push']

doWithReactor = {

	reactor('grailsReactor'){
		ext 'gorm', true
		ext 'browser', [
				'test' : true,
				'afterInsert' : [
						filter:TestDomain,
						browserFilter:{m,r-> true }
				],
				(R("sampleBro-.*")) : true,
		]
	}

	reactor(SharedConstants.PUSH_SCOPE){
		ext 'browser', [R("sampleBro-.*")]
	}
}