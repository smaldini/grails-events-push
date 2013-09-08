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
	public static final String TOPICS_HEADER       = "topics";
	public static final String GLOBAL_TOPIC        = "eventsbus";
	public static final String CLIENT_FILTER_PARAM = "browserFilter";
	public static final String PUSH_BODY           = "body";
	public static final String PUSH_TOPIC          = "topic";
	public static final String DELIMITER           = "<@>";

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

			Broadcaster b = BroadcasterFactory.getDefault().lookup(GLOBAL_TOPIC, true);
			if (b.getBroadcasterConfig().getBroadcasterCache() == null) {
				b.getBroadcasterConfig().setBroadcasterCache(new UUIDBroadcasterCache());
			}
			b.getBroadcasterConfig().addFilter(new PerRequestBroadcastFilter() {
				public BroadcastAction filter(AtmosphereResource atmosphereResource, Object originalMessage, Object message) {
					BroadcastSignal signal;

					String pass = null;

					if (BroadcastSignal.class.isAssignableFrom(message.getClass())) {
						signal = (BroadcastSignal) message;

						if (atmosphereResource.getRequest().getHeader(TOPICS_HEADER) != null) {
							String[] topics = atmosphereResource.getRequest().getHeader(TOPICS_HEADER).split(",");
							for (String topic : topics) {
								if (signal.selector.matches(topic)) {
									pass = topic;
									break;
								}
							}
						}

						if (signal.broadcastClientFilter != null && !(Boolean) signal.broadcastClientFilter.call(
								signal.eventMessageType ? signal.eventMessage : signal.eventMessage.getData(),
								atmosphereResource.getRequest()
						)) {
							pass = null;
						}

						if (null != pass) {
							return new BroadcastAction(jsonify(signal.eventMessage, pass));
						} else {
							return new BroadcastAction(BroadcastAction.ACTION.ABORT, null);
						}
					}

					return new BroadcastAction(message);
				}

				public BroadcastAction filter(Object originalMessage, Object message) {
					return new BroadcastAction(message);
				}
			});

			defineBridgeListener(grailsApplication, grailsEvents);
			registerTopics(grailsEvents);

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
	static public void registerTopics(EventsApi grailsEvents) {
		Collection<ReactorBuilder> pushBuilders =
				grailsEvents.getGroovyEnvironment().reactorBuildersByExtension(EventsPushScopes.TO_BROWSERS);

		Closure broadcastClientFilter;
		Broadcaster b = BroadcasterFactory.getDefault().lookup(GLOBAL_TOPIC);
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

					pushBuilder.get().on(selector, new BroadcastEventConsumer(selector, b, broadcastClientFilter));

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
		Broadcaster defaultBroadcaster = broadcasterFactory.lookup(GLOBAL_TOPIC);
		if (defaultBroadcaster == null) {
			res.sendError(403);
			return;
		}

		String header = req.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
		String _topics = req.getHeader(TOPICS_HEADER);
		if (_topics == null)
			return;

		// Create a Meteor
		Meteor m = Meteor.build(req);

		// Log all events on the console, including WebSocket events.
		if (log.isDebugEnabled()) {
			if (m.transport().equals(AtmosphereResource.TRANSPORT.WEBSOCKET))
				m.addListener(new WebSocketEventListenerAdapter());
			else
				m.addListener(new AtmosphereResourceEventListenerAdapter());
		}

		if (bridgeListener != null)
			m.addListener(bridgeListener);

		m.setBroadcaster(defaultBroadcaster);

		if (header != null && header.equalsIgnoreCase(HeaderConfig.LONG_POLLING_TRANSPORT)) {
			req.setAttribute(ApplicationConfig.RESUME_ON_BROADCAST, Boolean.TRUE);
		}

		m.suspend(-1);

	}

//    private void lookupTopic(String topic, Meteor m) {
//        Broadcaster b = broadcasterFactory.lookup(topic);
//        if (b != null) {
//            b.addAtmosphereResource(m.getAtmosphereResource());
//        } else {
//            Map.Entry<String, EventDefinition> whitelistedTopid = matchesWhitelist(topic);
//            if (whitelistedTopid != null) {
//                broadcasterFactory.lookup(topic, true).addAtmosphereResource(m.getAtmosphereResource());
//            }
//        }
//    }

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		Map element = (Map) new JsonSlurper().parse(new InputStreamReader(req.getInputStream()));

		String topic = element.containsKey(PUSH_TOPIC) ? element.get(PUSH_TOPIC).toString() : null;
		if (topic == null) {
			return;
		}

		if (element.containsKey(PUSH_BODY)) {
			grailsEvents.event(topic, element.get(PUSH_BODY), EventsPushScopes.FROM_BROWSERS, null, null, null);
		} else {
			grailsEvents.event(topic, element.get(PUSH_BODY), EventsPushScopes.FROM_BROWSERS, null, null, null);
		}
	}

//    private String extractTopic(String pathInfo) {
//        String[] decodedPath = pathInfo.split("/");
//        return decodedPath[decodedPath.length - 1];
//    }

}
