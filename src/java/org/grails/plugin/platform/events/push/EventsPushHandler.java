/* Copyright 2011-2012 the original author or authors:
 *
 *    Marc Palmer (marc@grailsrocks.com)
 *    Stéphane Maldini (stephane.maldini@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugin.platform.events.push;

import grails.converters.JSON;
import grails.events.GrailsEventsAware;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.MeteorService;
import org.atmosphere.cpr.*;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.grails.plugins.events.reactor.api.EventsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import reactor.event.Event;
import reactor.event.selector.Selector;
import reactor.event.selector.Selectors;
import reactor.function.Consumer;
import reactor.function.support.CancelConsumerException;
import reactor.groovy.config.ReactorBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephane Maldini <smaldini@gopivotal.com>
 * @version 1.0
 * @file
 * @date 16/05/12
 * @section DESCRIPTION
 * <p/>
 * [Does stuff]
 */
@MeteorService
public class EventsPushHandler extends HttpServlet {

	static private Logger log = LoggerFactory.getLogger(EventsPushHandler.class);

	private EventsApi          grailsEvents;
	private BroadcasterFactory broadcasterFactory;

	public static final String CONFIG_BRIDGE       = "grails.events.push.listener.bridge";
	public static final String CLIENT_FILTER_PARAM = "browserFilter";
	public static final String PUSH_BODY           = "body";
	public static final String PUSH_TOPIC          = "topic";
	public static final String DELIMITER           = "|";

	private AtmosphereResourceEventListener bridgeListener = null;

	@Override
	public void init() throws ServletException {
		super.init();

		broadcasterFactory = BroadcasterFactory.getDefault();
		ApplicationContext applicationContext = null;
		GrailsApplication grailsApplication = null;
		try {
			applicationContext =
					((ApplicationContext) getServletContext().getAttribute(ApplicationAttributes.APPLICATION_CONTEXT));
		} catch (Exception c) {
			log.error("Couldn't manage to retrieve appContext, servlet ordering problem ?", c);
		}

		if (applicationContext != null) {
			try {
				grailsEvents = applicationContext.getBean(EventsApi.class);
				grailsApplication = applicationContext.getBean(GrailsApplication.class);
			} catch (Exception c) {
				log.error("Couldn't manage to retrieve beans", c);
			}
		}

		if (grailsEvents != null) {
			defineBridgeListener(grailsApplication, grailsEvents);
			//b.scheduleFixedBroadcast(2 + DELIMITER + "{}", 10, TimeUnit.SECONDS);
		}

	}

	protected void defineBridgeListener(GrailsApplication application, EventsApi grailsEvents) {
		Object bridgeConfig = application.getConfig().flatten().get(CONFIG_BRIDGE);

		if (bridgeConfig == null)
			return;

		if (Boolean.class.isAssignableFrom(bridgeConfig.getClass()) && (Boolean) bridgeConfig) {
			bridgeListener = new BridgeWSListener();
		} else if (Class.class.isAssignableFrom(bridgeConfig.getClass())
				&& AtmosphereResourceEventListener.class.isAssignableFrom(((Class) bridgeConfig))) {
			try {
				bridgeListener = (AtmosphereResourceEventListener) ((Class) bridgeConfig).newInstance();
			} catch (InstantiationException e) {
				log.error("Failed to create listener", e);
			} catch (IllegalAccessException e) {
				log.error("Failed to find constructor for bridge listener", e);
			}
		}

		if (bridgeListener != null) {
			log.info("Bridge listener created, all browsers events will be dispatched to " + bridgeListener.toString());

			if (GrailsEventsAware.class.isAssignableFrom(bridgeListener.getClass())) {
				((GrailsEventsAware) bridgeListener).setGrailsEvents(grailsEvents);
				log.debug("Grails Events are successfully bridged to " + bridgeListener.toString());
			}
		}


	}

	@SuppressWarnings("unchecked")
	protected void registerTopics(Object topic, final AtmosphereResource resource) {
		Collection<ReactorBuilder> pushBuilders =
				grailsEvents.getGroovyEnvironment().reactorBuildersByExtension(EventsPushScopes.TO_BROWSERS);

		Closure broadcastClientFilter;
		Object cursor;
		Selector selector;
		Object key;
		Iterator iterableConfiguration;

		for (ReactorBuilder pushBuilder : pushBuilders) {
			cursor = pushBuilder.ext(EventsPushScopes.TO_BROWSERS);
			if (!Map.class.isAssignableFrom(cursor.getClass()) && !Collection.class.isAssignableFrom(cursor.getClass())) {
				continue;
			}

			iterableConfiguration = Map.class.isAssignableFrom(cursor.getClass()) ?
					((Map) cursor).entrySet().iterator() :
					((Collection) cursor).iterator();

			while (iterableConfiguration.hasNext()) {
				cursor = iterableConfiguration.next();
				broadcastClientFilter = null;

				if (Map.Entry.class.isAssignableFrom(cursor.getClass())) {
					key = ((Map.Entry) cursor).getKey();
					if (((Map.Entry) cursor).getValue() != null &&
							Map.class.isAssignableFrom(((Map.Entry) cursor).getValue().getClass())) {
						broadcastClientFilter = (Closure) ((Map) ((Map.Entry) cursor).getValue()).get(CLIENT_FILTER_PARAM);
					}
				} else {
					key = cursor;
				}

				if (key != null) {
					selector = Selector.class.isAssignableFrom(key.getClass()) ?
							(Selector) key :
							Selectors.$(key);

					if (selector.matches(topic)) {
						final Closure<Boolean> _broadcastClientFilter = broadcastClientFilter;
						pushBuilder.get().on(Selectors.$(topic), new Consumer<Event<?>>() {
							@Override
							public void accept(Event<?> event) {
								if (resource.isCancelled() || resource.isResumed()) {
									throw new CancelConsumerException();
								}
								if (_broadcastClientFilter == null || _broadcastClientFilter.call(event,
										resource.getRequest())) {
									resource.getBroadcaster().broadcast(jsonify(event, event.getKey()));
								}
							}
						});
					}

				}
			}
		}
	}

	private String jsonify(Event<?> message, Object key) {
		Map<String, Object> jsonResponse = new HashMap<String, Object>();
		jsonResponse.put(PUSH_TOPIC, key);
		jsonResponse.put(PUSH_BODY, message.getData());
		String res = new JSON(jsonResponse).toString();
		return res.length() + DELIMITER + res;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		Meteor m = Meteor.build(req, Broadcaster.SCOPE.REQUEST, null, null);

		if (m.getBroadcaster().getBroadcasterConfig().getBroadcasterCache() == null) {
			m.getBroadcaster().getBroadcasterConfig().setBroadcasterCache(new UUIDBroadcasterCache());
		}

		// Log all events on the console, including WebSocket events.
		if (bridgeListener != null)
			m.addListener(bridgeListener);

		m.resumeOnBroadcast(m.transport() == AtmosphereResource.TRANSPORT.LONG_POLLING).suspend(-1);

	}

	public void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
		Map element = (Map) new JsonSlurper().parse(new InputStreamReader(req.getInputStream()));
		req.getInputStream().close();

		final String topic = element.containsKey(PUSH_TOPIC) ? element.get(PUSH_TOPIC).toString() : null;
		if (topic == null) {
			return;
		}

		if (element.containsKey(PUSH_BODY)) {
			grailsEvents.event(topic, element.get(PUSH_BODY), EventsPushScopes.FROM_BROWSERS, null, null, null);
		} else {
			boolean breakingBad = false;
			AtmosphereResource targetResource = null;
			for (Broadcaster broadcaster : broadcasterFactory.lookupAll()) {
				for (AtmosphereResource resource : broadcaster.getAtmosphereResources()) {
					if (resource.uuid().equalsIgnoreCase(((AtmosphereRequest) req).resource().uuid())) {
						targetResource = resource;
						breakingBad = true;
						break;
					}
				}
				if (breakingBad) {
					break;
				}
			}
			if (targetResource != null)
				registerTopics(topic, targetResource);
		}
	}
}
