package grails.events;

import org.grails.plugin.platform.events.Events;

/**
 * Author: smaldini
 * Date: 1/21/13
 * Project: events-push
 */
public interface GrailsEventsAware {

    void setGrailsEvents(Events grailsEvents);
}
