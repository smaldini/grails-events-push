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

import grails.events.BroadcastOrder;
import org.atmosphere.cpr.*;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;
import org.codehaus.groovy.grails.commons.ApplicationAttributes;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.grails.plugin.platform.events.EventDefinition;
import org.grails.plugin.platform.events.EventReply;
import org.grails.plugin.platform.events.EventsImpl;
import org.grails.plugin.platform.events.registry.EventsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    private EventsImpl grailsEvents;
    private EventsRegistry eventsRegistry;
    private GrailsApplication grailsApplication;
    private BroadcasterFactory broadcasterFactory;

    public static final String ID_GRAILSEVENTS = "grailsEvents";
    public static final String PUSH_SCOPE = "browser";
    public static final String CLIENT_BROADCAST_PARAM = "clientBroadcast";
    public static final String MESSAGE_PARAM = "message";

    @Override
    public void init() throws ServletException {
        super.init();

        broadcasterFactory = BroadcasterFactory.getDefault();

        ApplicationContext applicationContext = null;
        try {
            applicationContext =
                    ((ApplicationContext) getServletContext().getAttribute(ApplicationAttributes.APPLICATION_CONTEXT));
        } catch (Exception c) {
            log.error("Couldn't manage to retrieve appContext, servlet ordering problem ?", c);
        }

        if (applicationContext != null) {
            try {
                grailsApplication = applicationContext.getBean("grailsApplication", GrailsApplication.class);
                grailsEvents = applicationContext.getBean(ID_GRAILSEVENTS, EventsImpl.class);
                eventsRegistry = grailsEvents.getGrailsEventsRegistry();
            } catch (Exception c) {
                log.error("Couldn't manage to retrieve beans", c);
            }
        }

        if (grailsEvents != null && eventsRegistry != null) {
            registerTopics(eventsRegistry, grailsEvents);
        }
    }

    static public void registerTopics(EventsRegistry eventsRegistry, EventsImpl grailsEvents) {
        Method m = ReflectionUtils.findMethod(BroadcastEventWrapper.class, "broadcastEvent", Object.class);
        List<String> doneTopics = new ArrayList<String>();
        Object broadcastClient = null;

        for (EventDefinition eventDefinition : grailsEvents.getEventDefinitions()) {
            String topic = eventDefinition.getListenerId().getTopic();
            broadcastClient = eventDefinition.getOthersAttributes() != null ? eventDefinition.getOthersAttributes().get(CLIENT_BROADCAST_PARAM) : false;
            broadcastClient = broadcastClient != null ? broadcastClient : false;

            if (topic != null && ((Boolean) broadcastClient || eventDefinition.getScope().equalsIgnoreCase(PUSH_SCOPE)) &&
                    !doneTopics.contains(topic)) {
                eventsRegistry.addListener(PUSH_SCOPE, topic, new BroadcastEventWrapper(topic, (Boolean) broadcastClient), m);
                doneTopics.add(topic);
            }
        }
    }

    private static class BroadcastEventWrapper {
        private Broadcaster b;
        private boolean clientBroadcast = false;

        public BroadcastEventWrapper(String topic, boolean clientBroadcast) {
            this.b = BroadcasterFactory.getDefault().lookup(topic, true);
            this.clientBroadcast = clientBroadcast;
        }

        public void broadcastEvent(Object message) {
            if (message != null && (clientBroadcast || !message.getClass().isAssignableFrom(AtmosphereRequest.class))) {
                if (message.getClass().isAssignableFrom(AtmosphereRequest.class)) {
                    try {
                        StringBuffer sb = new StringBuffer();
                        String buffer = null;
                        while ((buffer = ((AtmosphereRequest) message).getReader().readLine()) != null) {
                            sb.append(buffer);
                        }
                        message = sb.toString();
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }

                b.broadcast(message);
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Create a Meteor
        Meteor m = Meteor.build(req);

        // Log all events on the console, including WebSocket events.
        if (log.isDebugEnabled())
            m.addListener(new WebSocketEventListenerAdapter());

        //res.setContentType("text/html;charset=ISO-8859-1");


        Broadcaster b = broadcasterFactory.lookup(extractTopic(req.getPathInfo()));
        if(b == null){
            res.sendError(403);
            return;
        }

        m.setBroadcaster(b);

        String header = req.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
        if (header != null && header.equalsIgnoreCase(HeaderConfig.LONG_POLLING_TRANSPORT)) {
            req.setAttribute(ApplicationConfig.RESUME_ON_BROADCAST, Boolean.TRUE);
            m.suspend(-1, false);
        } else {
            m.suspend(-1);
        }

    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String topic = extractTopic(req.getPathInfo());
        EventReply reply = grailsEvents._event(PUSH_SCOPE, topic, req);
        if (reply.size() > 0) {
            List<String> toBroadcast = new ArrayList<String>();
            try {
                for (Object data : reply.getValues()) {
                    if (BroadcastOrder.class.isAssignableFrom(data.getClass())) {
                        toBroadcast.add(((BroadcastOrder)data).getData().toString());
                    }
                }
            } catch (ExecutionException e) {
                log.error("", e);
            } catch (InterruptedException e) {
                log.error("", e);
            }
            if (toBroadcast.size() > 0) {
                Broadcaster b = broadcasterFactory.lookup(topic);
                b.broadcast(toBroadcast);
            }

        }

    }

    private String extractTopic(String pathInfo) {
        String[] decodedPath = pathInfo.split("/");
        return decodedPath[decodedPath.length - 1];
    }
}
