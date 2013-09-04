package org.grails.plugin.platform.events.push;

import groovy.lang.Closure;
import reactor.event.Event;
import reactor.event.selector.Selector;

public class BroadcastSignal<T> {
	final boolean  eventMessageType;
	final Selector selector;
	final Event<T> eventMessage;
	final Closure  broadcastClientFilter;

	public BroadcastSignal(Selector selector, Event<T> message, boolean eventMessageType, Closure broadcastClientFilter) {
		this.eventMessage = message;
		this.eventMessageType = eventMessageType;
		this.selector = selector;
		this.broadcastClientFilter = broadcastClientFilter;
	}
}