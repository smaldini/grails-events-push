package org.grails.plugin.platform.events.push;

/**
 * Author: smaldini
 * Date: 1/15/13
 * Project: events-push
 */
public class EventsPushConstants {
	public static final String FROM_BROWSERS = "browser";
	public static final String TO_BROWSERS   = FROM_BROWSERS;

	public static final String CONFIG_BRIDGE = "grails.events.push.listener.bridge";

	public static final String CLIENT_FILTER_PARAM = "browserFilter";

	public static final String TOPICS_HEADER           = "topics";
	public static final String TOPICS_HEADER_DELIMITER = "%2C";

	public static final String GLOBAL_TOPIC = "/*";

	public static final byte[] END_MESSAGE     = "END".getBytes();
	public static final char   DELIMITER       = '|';
	public static final String TYPE_STRING     = "0";
	public static final String TYPE_JSON       = "1";
	public static final String TYPE_BINARY     = "2";
	public static final String TYPE_REGISTER   = "4";
	public static final String TYPE_UNREGISTER = "5";
}
