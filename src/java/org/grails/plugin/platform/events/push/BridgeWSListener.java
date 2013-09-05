package org.grails.plugin.platform.events.push;

import grails.events.GrailsEventsAware;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListener;
import org.grails.plugins.events.reactor.api.EventsApi;

/**
 * Author: smaldini
 * Date: 1/15/13
 * Project: events-push
 */
public class BridgeWSListener implements WebSocketEventListener, GrailsEventsAware {

	private EventsApi grailsEvents;

	public void setGrailsEvents(EventsApi events) {
		this.grailsEvents = events;
	}

	@Override
	public void onHandshake(WebSocketEvent event) {
		grailsEvents.event("onHandshake", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onMessage(WebSocketEvent event) {
		grailsEvents.event("onMessage", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onClose(WebSocketEvent event) {
		grailsEvents.event("onClose", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onControl(WebSocketEvent event) {
		grailsEvents.event("onControl", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onDisconnect(WebSocketEvent event) {
		grailsEvents.event("onDisconnect", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onConnect(WebSocketEvent event) {
		grailsEvents.event("onConnect", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onPreSuspend(AtmosphereResourceEvent event) {
		//grailsEvents.event("onPreSuspend", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onSuspend(AtmosphereResourceEvent event) {
		grailsEvents.event("onConnect", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onResume(AtmosphereResourceEvent event) {
		//grailsEvents.event("onResume", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onDisconnect(AtmosphereResourceEvent event) {
		grailsEvents.event("onDisconnect", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onBroadcast(AtmosphereResourceEvent event) {
		//grailsEvents.event("onBroadcast", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}

	@Override
	public void onThrowable(AtmosphereResourceEvent event) {
		//grailsEvents.event("onThrowable", event, SharedConstants.PUSH_SCOPE, null, null, null);
	}
}
