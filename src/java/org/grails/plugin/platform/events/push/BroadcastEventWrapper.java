package org.grails.plugin.platform.events.push;

import groovy.lang.Closure;
import org.atmosphere.cpr.Broadcaster;
import org.grails.plugin.platform.events.EventMessage;

public class BroadcastEventWrapper {
    private boolean eventMessageType = false;
    private Broadcaster b;
    private Closure broadcastClientFilter;

    public BroadcastEventWrapper(final Broadcaster b, final Closure broadcastClientFilter) {
        this.b = b;

        if (broadcastClientFilter != null) {
            this.broadcastClientFilter = broadcastClientFilter;
            this.eventMessageType = EventMessage.class.isAssignableFrom(broadcastClientFilter.getParameterTypes()[0]);
        }
    }

    public void broadcastEvent(EventMessage message) {
        b.broadcast(new BroadcastSignal(message, eventMessageType, broadcastClientFilter));
    }
}