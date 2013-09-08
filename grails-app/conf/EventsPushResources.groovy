modules = {
    'atmosphere' {
        resource id:'js', url:[plugin: 'events-push', dir:'js/atmosphere', file:"atmosphere.js"],
            disposition:'head'
    }
		'atmosphere-jquery' {
        dependsOn 'jquery'
        resource id:'js', url:[plugin: 'events-push', dir:'js/atmosphere', file:"jquery.atmosphere.js"],
            disposition:'head'
    }

    'grailsEvents' {
        dependsOn 'atmosphere'
        resource id:'js', url:[plugin: 'events-push', dir:'js/grails', file:"grailsEvents.js"]
    }

}