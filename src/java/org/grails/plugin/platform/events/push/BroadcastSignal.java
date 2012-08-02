package org.grails.plugin.platform.events.push;

import groovy.lang.Closure;
import org.grails.plugin.platform.events.EventMessage;

public class BroadcastSignal {
    boolean eventMessageType = false;
    EventMessage eventMessage;
    Closure broadcastClientFilter;

    public BroadcastSignal(EventMessage message, boolean eventMessageType, Closure broadcastClientFilter) {
        this.eventMessage = message;
        this.eventMessageType = eventMessageType;
        this.broadcastClientFilter = broadcastClientFilter;
    }
}