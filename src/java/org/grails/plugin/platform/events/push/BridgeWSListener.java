package org.grails.plugin.platform.events.push;

import grails.events.GrailsEventsAware;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListener;
import org.grails.plugin.platform.events.Events;
import org.grails.plugin.platform.events.publisher.EventsPublisher;

/**
 * Author: smaldini
 * Date: 1/15/13
 * Project: events-push
 */
public class BridgeWSListener implements WebSocketEventListener, GrailsEventsAware {

    private Events grailsEvents;

    public void setGrailsEvents(Events events) {
        this.grailsEvents = events;
    }

    @Override
    public void onHandshake(WebSocketEvent event) {
        grailsEvents.event(SharedConstants.PUSH_SCOPE, "onHandshake", event);
    }

    @Override
    public void onMessage(WebSocketEvent event) {
        grailsEvents.event(SharedConstants.PUSH_SCOPE, "onMessage", event);
    }

    @Override
    public void onClose(WebSocketEvent event) {
        grailsEvents.event(SharedConstants.PUSH_SCOPE, "onClose", event);
    }

    @Override
    public void onControl(WebSocketEvent event) {
        grailsEvents.event(SharedConstants.PUSH_SCOPE, "onControl", event);
    }

    @Override
    public void onDisconnect(WebSocketEvent event) {
        grailsEvents.event(SharedConstants.PUSH_SCOPE, "onDisconnect", event);
    }

    @Override
    public void onConnect(WebSocketEvent event) {
        grailsEvents.event(SharedConstants.PUSH_SCOPE, "onConnect", event);
    }

    @Override
    public void onPreSuspend(AtmosphereResourceEvent event) {
        //grailsEvents.event(SharedConstants.PUSH_SCOPE, "onPreSuspend", event);
    }

    @Override
    public void onSuspend(AtmosphereResourceEvent event) {
        //grailsEvents.event(SharedConstants.PUSH_SCOPE, "onConnect", event);
    }

    @Override
    public void onResume(AtmosphereResourceEvent event) {
        //grailsEvents.event(SharedConstants.PUSH_SCOPE, "onResume", event);
    }

    @Override
    public void onDisconnect(AtmosphereResourceEvent event) {
        //grailsEvents.event(SharedConstants.PUSH_SCOPE, "onDisconnect", event);
    }

    @Override
    public void onBroadcast(AtmosphereResourceEvent event) {
        //grailsEvents.event(SharedConstants.PUSH_SCOPE, "onBroadcast", event);
    }

    @Override
    public void onThrowable(AtmosphereResourceEvent event) {
        //grailsEvents.event(SharedConstants.PUSH_SCOPE, "onThrowable", event);
    }
}
