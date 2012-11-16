package org.grails.plugin.platform.events.push


import org.atmosphere.cpr.*
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.grails.plugin.platform.events.registry.EventsRegistry;
import org.grails.plugin.platform.events.*;
import grails.converters.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import static org.grails.plugin.platform.events.push.SharedConstants.*;

class PushPerRequestBroadcastFilter implements PerRequestBroadcastFilter {
  
    static private Logger log = LoggerFactory.getLogger(PushPerRequestBroadcastFilter.class);
    private ApplicationContext applicationContext = null;
    private Events grailsEvents;
    private EventsRegistry eventsRegistry;
    private PersistenceContextInterceptor persistenceInterceptor

    public PushPerRequestBroadcastFilter(def applicationContext, def grailsEvents, def eventsRegistry) {
        this.applicationContext = applicationContext
        this.grailsEvents = grailsEvents
        this.eventsRegistry = eventsRegistry  
        try {
            persistenceInterceptor = applicationContext.getBean("persistenceInterceptor", PersistenceContextInterceptor.class);
        } catch (Exception c) {
            log.error "Couldn't manage to load persistence interceptor bean", c
        }
    }

    public BroadcastAction filter(AtmosphereResource atmosphereResource, Object originalMessage, Object message) {
        BroadcastSignal signal;

        Boolean pass = false;

        if (BroadcastSignal.class.isAssignableFrom(message.getClass())) {
          try {
            // Required or will see the following error:
            // org.hibernate.LazyInitializationException: could not initialize proxy - no Session
            persistenceInterceptor.init(); 
            persistenceInterceptor.setReadOnly()                       
            
            signal = (BroadcastSignal) message;
            def eventMessage = signal.eventMessage
            def dataMessage = eventMessage.getData()

            if (eventMessage.isGormSession()) {
              // To call merge is not enough here as it doesn't seem to process associated collections 
              // This call is required to avoid seeing the following error:
              // org.hibernate.HibernateException: illegally attempted to associate a proxy with two open Sessions
              dataMessage = dataMessage.get(dataMessage.id) 
            } 

            if (atmosphereResource.getRequest().getHeader(TOPICS_HEADER) != null) {
                String[] topics = atmosphereResource.getRequest().getHeader(TOPICS_HEADER).split(",");
                for (String topic : topics) {
                    if (topic.equals(signal.eventMessage.getEvent())) {
                        pass = true;
                        break;
                    }
                }
            }

            if (signal.broadcastClientFilter != null) {
                pass = (Boolean) signal.broadcastClientFilter.call(
                        [signal.eventMessageType ? eventMessage : dataMessage, atmosphereResource.getRequest()] as Object[]
                  );
            }

            if (pass) {
                String json = jsonify(eventMessage, dataMessage);
                return new BroadcastAction(json);
            } else {
                return new BroadcastAction(BroadcastAction.ACTION.ABORT, null);
            }
          } catch (Exception e){
            log.error "Exception in Atmosphere Request Filter", e
          } finally {
            persistenceInterceptor.flush();
            persistenceInterceptor.destroy();
          }
        }
        return new BroadcastAction(message);
    }

    public BroadcastAction filter(Object originalMessage, Object message) {
        return new BroadcastAction(message);
    }

    private String jsonify(EventMessage eventMessage, def dataMessage) {
        Map<String, Object> jsonResponse = new HashMap<String, Object>();
        jsonResponse.put("topic", eventMessage.getEvent());
        jsonResponse.put("body", dataMessage);
        String res = new JSON(jsonResponse).toString();
        return res.length() + DELIMITER + res;
    }

}
