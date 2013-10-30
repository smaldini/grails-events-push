package org.grails.plugin.platform.events.push;

import grails.converters.JSON;
import groovy.lang.Closure;
import org.apache.commons.io.IOUtils;
import org.atmosphere.cpr.AtmosphereResource;
import reactor.event.Event;
import reactor.function.Consumer;
import reactor.function.support.CancelConsumerException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.grails.plugin.platform.events.push.EventsPushConstants.*;


/**
 * @author Stephane Maldini
 */
public class AtmosphereConsumer implements Consumer<Event<?>> {

	final AtmosphereResource resource;
	final Closure<Boolean>   broadcastClientFilter;
	final boolean            isBinary;

	public AtmosphereConsumer(AtmosphereResource resource, Closure<Boolean> broadcastClientFilter) {
		this.resource = resource;
		this.broadcastClientFilter = broadcastClientFilter;
		this.isBinary = resource.forceBinaryWrite();
	}

	@Override
	public void accept(Event<?> event) {
		if (resource.isCancelled() || resource.isResumed()) {
			throw new CancelConsumerException();
		}
		if (broadcastClientFilter == null || broadcastClientFilter.call(event,
				resource.getRequest())) {

			try {
				OutputStream outputStream = resource.getResponse().getOutputStream();

				outputMessage(event, outputStream);
				outputStream.flush();

				switch (resource.transport()) {
					case JSONP:
					case AJAX:
					case LONG_POLLING:
						if (resource.isSuspended())
							resource.resume();
						break;
				}
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}
	}

	private void outputMessage(Event<?> message, OutputStream outputStream) throws IOException {
		final Object data = message.getData();
		String res = message.getKey().toString() + DELIMITER;
		if (data != null) {
			if (InputStream.class.isAssignableFrom(data.getClass())) {
				outputStream.write((res + TYPE_BINARY + DELIMITER).getBytes());
				IOUtils.copy(((InputStream)data), outputStream);
				((InputStream)data).close();
				outputStream.write(END_MESSAGE);
			} else {
				if (String.class.isAssignableFrom(data.getClass())) {
					res = res + TYPE_STRING + DELIMITER + data;
				} else {
					res = res + TYPE_JSON + DELIMITER + new JSON(data).toString();
				}
				outputStream.write(res.getBytes());
				if(isBinary){
					outputStream.write(END_MESSAGE);
				}
			}

		}
	}
}
