package grails.events;

import org.grails.plugins.events.reactor.api.EventsApi;

/**
 * Author: smaldini
 */
public interface GrailsEventsAware {

    void setGrailsEvents(EventsApi grailsEvents);
}
