modules = {
    'atmosphere' {
        dependsOn 'jquery'
        resource id:'js', url:[plugin: 'events-push', dir:'js/jquery', file:"jquery.atmosphere.js"],
            disposition:'head'
    }

    'grailsEvents' {
        dependsOn 'atmosphere'
        resource id:'js', url:[plugin: 'events-push', dir:'js/grails', file:"grailsEvents.js"]
    }

}