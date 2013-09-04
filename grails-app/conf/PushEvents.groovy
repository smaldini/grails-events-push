import org.grails.plugin.platform.events.push.SharedConstants

includes = ['default']

doWithReactor = {

	reactor(SharedConstants.PUSH_SCOPE){ }
}