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
import org.apache.commons.io.IOUtils;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.MeteorService;
import org.atmosphere.cpr.*;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.grails.plugins.events.reactor.api.EventsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import reactor.event.Event;
import reactor.event.registry.Registration;
import reactor.event.selector.Selector;
import reactor.event.selector.Selectors;
import reactor.function.Consumer;
import reactor.function.support.CancelConsumerException;
import reactor.groovy.config.ReactorBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

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
	public static final byte[] DELIMITER           = "END".getBytes();
	public static final String GLOBAL_TOPIC        = "eventbus";
	public static final String TYPE_REGISTER       = "4";
	public static final String TYPE_UNREGISTER     = "5";
	public static final String TYPE_RAW            = "0";
	public static final String TYPE_JSON           = "1";
	public static final String TYPE_BINARY         = "2";

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
			Broadcaster b = broadcasterFactory.lookup(GLOBAL_TOPIC, true);
			if (b.getBroadcasterConfig().getBroadcasterCache() == null) {
				b.getBroadcasterConfig().setBroadcasterCache(new UUIDBroadcasterCache());
			}

			defineBridgeListener(grailsApplication, grailsEvents);
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
	protected Set<Registration<?>> registerTopics(Object topic, final AtmosphereResource resource) {
		Collection<ReactorBuilder> pushBuilders =
				grailsEvents.getGroovyEnvironment().reactorBuildersByExtension(EventsPushScopes.TO_BROWSERS);

		Closure broadcastClientFilter;
		Object cursor;
		Selector selector;
		Object key;
		Iterator iterableConfiguration;
		Set<Registration<?>> registrations = new HashSet<Registration<?>>();

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
						registrations.add(pushBuilder.get().on(Selectors.$(topic), new Consumer<Event<?>>() {
							@Override
							public void accept(Event<?> event) {
								if (resource.isCancelled() || resource.isResumed()) {
									throw new CancelConsumerException();
								}
								if (_broadcastClientFilter == null || _broadcastClientFilter.call(event,
										resource.getRequest())) {

									try {
										OutputStream outputStream = resource.getResponse().getOutputStream();

										outputMessage(event, outputStream);
										outputStream.flush();

										switch (resource.transport()) {
											case JSONP:
											case AJAX:
											case LONG_POLLING:
												if (resource.isSuspended()) ;
												resource.resume();
												break;
										}
									} catch (IOException exception) {
										exception.printStackTrace();
									}
									//resource.getBroadcaster().broadcast();
								}
							}
						}));
					}
				}
			}
		}
		return registrations;
	}

	private void outputMessage(Event<?> message, OutputStream outputStream) throws IOException {
		final Object data = message.getData();
		String res = message.getKey().toString() + "|";
		if (data != null) {
			if (InputStream.class.isAssignableFrom(data.getClass())) {
				ByteArrayOutputStream bous = new ByteArrayOutputStream();
				IOUtils.copy((InputStream)data, bous);

				outputStream.write((res + TYPE_BINARY + "|").getBytes());
				outputStream.write(bous.toByteArray());
				outputStream.write(DELIMITER);
			} else {
				if (String.class.isAssignableFrom(data.getClass())) {
					res = res + TYPE_RAW + "|" + data;
				} else {
					res = res + TYPE_JSON + "|" + new JSON(data).toString();
				}
				outputStream.write(res.getBytes());
			}

		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		Meteor m = Meteor.build(req);
		Broadcaster b = broadcasterFactory.lookup(GLOBAL_TOPIC);
		req.setAttribute(AtmosphereResourceImpl.SKIP_BROADCASTER_CREATION, Boolean.TRUE);

		// Log all events on the console, including WebSocket events.
		if (bridgeListener != null)
			m.addListener(bridgeListener);

		m.resumeOnBroadcast(m.transport() == AtmosphereResource.TRANSPORT.LONG_POLLING).suspend(-1);
		b.addAtmosphereResource(m.getAtmosphereResource());

		if (req.getHeader("topics") != null) {
			Set<Registration<?>> registrations = new HashSet<Registration<?>>();
			for (String topic : req.getHeader("topics").split("%2C")) {
				registrations.addAll(registerTopics(topic, m.getAtmosphereResource()));
			}
			m.getAtmosphereResource().addEventListener(new AtmosphereRegistrationsHandler(registrations));
		}
	}

	public void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException {


		final String topic = readPacket(req.getInputStream());
		if (topic == null || topic.isEmpty()) {
			return;
		}

		final String type = readPacket(req.getInputStream());
		if (type == null || type.isEmpty()) {
			return;
		}

		if (type.equals(TYPE_RAW)) {
			grailsEvents.event(topic, readPacket(req.getInputStream()), EventsPushScopes.FROM_BROWSERS, null, null, null);
		} else if (type.equals(TYPE_JSON)) {
			grailsEvents.event(topic,
					new JsonSlurper().parse(new BufferedReader(new InputStreamReader(req.getInputStream()))),
					EventsPushScopes.FROM_BROWSERS, null, null, null
			);
		} else if (type.equals(TYPE_BINARY)) {
			grailsEvents.event(topic, req.getInputStream(), EventsPushScopes.FROM_BROWSERS, null, null, null);
		} else if (type.equals(TYPE_REGISTER)) {
			AtmosphereResource targetResource = null;
			for (AtmosphereResource resource : broadcasterFactory.lookup(GLOBAL_TOPIC).getAtmosphereResources()) {
				if (resource.uuid().equalsIgnoreCase(((AtmosphereRequest) req).resource().uuid())) {
					targetResource = resource;
					break;
				}
			}
			if (targetResource != null) {
				targetResource.addEventListener(new AtmosphereRegistrationsHandler(registerTopics(topic, targetResource)));
			}
		} else if (type.equals(TYPE_UNREGISTER)) {
		}
	}

	private String readPacket(InputStream is) throws IOException {
		StringBuffer res = new StringBuffer();
		int i = 0;
		while (-1 != (i = is.read())) {
			if (((char) i) == '|')
				break;
			res.append((char) i);
		}
		return res.toString();
	}
}
