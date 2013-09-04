package org.grails.plugin.platform.events.push;

import groovy.lang.Closure;
import org.atmosphere.cpr.Broadcaster;
import reactor.event.Event;
import reactor.event.selector.Selector;
import reactor.function.Consumer;

public class BroadcastEventConsumer implements Consumer<Event<?>> {
	private boolean eventMessageType = false;
	private Broadcaster b;
	private Closure     broadcastClientFilter;
	private Selector    selector;

	public BroadcastEventConsumer(final Selector selector, final Broadcaster b, final Closure broadcastClientFilter) {
		this.b = b;
		this.selector = selector;

		if (broadcastClientFilter != null) {
			this.broadcastClientFilter = broadcastClientFilter;
			this.eventMessageType = Event.class.isAssignableFrom(broadcastClientFilter.getParameterTypes()[0]);
		}
	}

	@SuppressWarnings("unchecked")
	public void accept(Event<?> message) {
		b.broadcast(new BroadcastSignal(selector, message, eventMessageType, broadcastClientFilter));
	}
}