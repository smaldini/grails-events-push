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


import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import org.apache.commons.io.IOUtils;
import org.atmosphere.cache.HeaderBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeFilter;
import org.atmosphere.cpr.*;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.json.JSONElement;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.grails.plugin.platform.events.*;
import org.grails.plugin.platform.events.registry.EventsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.grails.plugin.platform.events.push.SharedConstants.*;

/**
 * @author Stephane Maldini <smaldini@doc4web.com>
 * @version 1.0
 * @file
 * @date 16/05/12
 * @section DESCRIPTION
 * <p/>
 * [Does stuff]
 */
public class EventsPushHandler extends HttpServlet {

    static private Logger log = LoggerFactory.getLogger(EventsPushHandler.class);
    
    private ApplicationContext applicationContext = null;
    private Events grailsEvents;
    private EventsRegistry eventsRegistry;
    private BroadcasterFactory broadcasterFactory;

    public static final String ID_GRAILSEVENTS = "grailsEvents";
    public static final String ID_GRAILSEVENTSREGISTRY = "grailsEventsRegistry";
    public static final String GLOBAL_TOPIC = "eventsbus";
    public static final String PUSH_SCOPE = "browser";
    public static final String CLIENT_BROADCAST_PARAM = "browser";
    public static final String CLIENT_FILTER_PARAM = "browserFilter";

    public HashMap<String, EventDefinition> broadcastersWhiteList = new HashMap<String, EventDefinition>();

    private static final Method broadcastEventMethod = ReflectionUtils.findMethod(BroadcastEventWrapper.class, "broadcastEvent", EventMessage.class);

    @Override
    public void init() throws ServletException {
        super.init();

        broadcasterFactory = BroadcasterFactory.getDefault();
        try {
            applicationContext =
                    ((ApplicationContext) getServletContext().getAttribute(ApplicationAttributes.APPLICATION_CONTEXT));
        } catch (Exception c) {
            log.error("Couldn't manage to retrieve appContext, servlet ordering problem ?", c);
        }

        if (applicationContext != null) {
            try {
                grailsEvents = applicationContext.getBean(ID_GRAILSEVENTS, EventsImpl.class);
                eventsRegistry = applicationContext.getBean(ID_GRAILSEVENTSREGISTRY, EventsRegistry.class);
            } catch (Exception c) {
                log.error("Couldn't manage to retrieve beans", c);
            }
        }

        if (grailsEvents != null && eventsRegistry != null) {
            Broadcaster b = BroadcasterFactory.getDefault().lookup(GLOBAL_TOPIC, true);
            b.getBroadcasterConfig().setBroadcasterCache(new HeaderBroadcasterCache());
            b.getBroadcasterConfig().addFilter(new PushPerRequestBroadcastFilter(applicationContext, grailsEvents, eventsRegistry));                           
            broadcastersWhiteList.putAll(registerTopics(eventsRegistry, grailsEvents));
            b.scheduleFixedBroadcast(2+DELIMITER+"{}", 10, TimeUnit.SECONDS);
        }

    }

    static public Map<String, EventDefinition> registerTopics(EventsRegistry eventsRegistry, Events grailsEvents) {
        Map<String, EventDefinition> doneTopics = new HashMap<String, EventDefinition>();
        Object broadcastClient;
        Closure broadcastClientFilter;
        Broadcaster b = BroadcasterFactory.getDefault().lookup(GLOBAL_TOPIC);

        for (EventDefinition eventDefinition : grailsEvents.getEventDefinitions()) {
            String topic = eventDefinition.getTopic();
            broadcastClient = null;
            broadcastClientFilter = null;

            if (eventDefinition.getOthersAttributes() != null) {
                broadcastClient = eventDefinition.getOthersAttributes().get(CLIENT_BROADCAST_PARAM);
                broadcastClientFilter = (Closure) eventDefinition.getOthersAttributes().get(CLIENT_FILTER_PARAM);
            }

            broadcastClient = broadcastClient != null ? broadcastClient : false;

            if (topic != null && ((Boolean) broadcastClient) &&
                    !doneTopics.containsKey(topic)) {
                eventsRegistry.on(eventDefinition.getNamespace(), topic, new BroadcastEventWrapper(b, broadcastClientFilter), broadcastEventMethod);

                doneTopics.put(topic, eventDefinition);
            }
        }
        return doneTopics;
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

//        String[] topics = _topics.split(",");

        // Create a Meteor
        Meteor m = Meteor.build(req);

//        for (String topic : topics) {
//            if (topic.equals(GLOBAL_TOPIC))
//                continue;
//
//            lookupTopic(topic, m);
//        }

        // Log all events on the console, including WebSocket events.
        if (log.isDebugEnabled()) {
            if (m.transport().equals(AtmosphereResource.TRANSPORT.WEBSOCKET))
                m.addListener(new WebSocketEventListenerAdapter());
            else
                m.addListener(new AtmosphereResourceEventListenerAdapter());
        }

        res.setContentType("application/javascript; charset=UTF-8");

        m.setBroadcaster(defaultBroadcaster);

        if (header != null && header.equalsIgnoreCase(HeaderConfig.LONG_POLLING_TRANSPORT)) {
            req.setAttribute(ApplicationConfig.RESUME_ON_BROADCAST, Boolean.TRUE);
        }
        m.suspend(-1, false);

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

    private Map.Entry<String, EventDefinition> matchesWhitelist(String topic) {
        for (Map.Entry<String, EventDefinition> whitelistedTopic : broadcastersWhiteList.entrySet()) {
            if (ListenerId.matchesTopic(whitelistedTopic.getKey(), topic, false))
                return whitelistedTopic;
        }

        return null;
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Map element = (Map)new JsonSlurper().parse(new InputStreamReader(req.getInputStream()));

        String topic = element.containsKey("topic") ? element.get("topic").toString() : null;
        if (topic == null) {
            return;
        }
        final Object body = element.containsKey("body") ? element.get("body") : null;
        grailsEvents.event(PUSH_SCOPE, topic, body != null ? body : element);
    }

//    private String extractTopic(String pathInfo) {
//        String[] decodedPath = pathInfo.split("/");
//        return decodedPath[decodedPath.length - 1];
//    }

}
