package org.grails.plugin.platform.events.push;

import static org.atmosphere.cpr.ApplicationConfig.MAPPING;
import static org.atmosphere.cpr.ApplicationConfig.SERVLET_CLASS;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.atmosphere.cpr.MeteorServlet;
import org.atmosphere.handler.ReflectorServletProcessor;

/**
 * @author Stephane Maldini <smaldini@doc4web.com>
 * @version 1.0
 * @file
 * @date 26/05/12
 * @section DESCRIPTION
 * <p/>
 * [Does stuff]
 */
public class GrailsMeteorServlet extends MeteorServlet {

    @Override
    public void init(ServletConfig sc) throws ServletException {

        String servletClass = framework().getAtmosphereConfig().getInitParameter(SERVLET_CLASS);
        if (servletClass == null) {
            ReflectorServletProcessor r = new ReflectorServletProcessor(new EventsPushHandler());
            r.setServletClassName(EventsPushHandler.class.getName());
            String mapping = framework().getAtmosphereConfig().getInitParameter(MAPPING);

            if (mapping == null) {
                mapping = "/*";
            }
            framework.addAtmosphereHandler(mapping, r);
        }
        super.init(sc);
    }
}
