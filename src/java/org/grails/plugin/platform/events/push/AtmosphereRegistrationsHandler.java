package org.grails.plugin.platform.events.push;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import reactor.event.registry.Registration;

import java.util.Collection;

/**
 * @author Stephane Maldini
 */
public class AtmosphereRegistrationsHandler implements AtmosphereResourceEventListener {

	final Collection<Registration<?>> registrations;

	public AtmosphereRegistrationsHandler(Collection<Registration<?>> registrations) {
		this.registrations = registrations;
	}

	private void cancel() {
		for (Registration<?> registration : registrations) {
			registration.cancel();
		}
	}

	@Override
	public void onPreSuspend(AtmosphereResourceEvent event) {

	}

	@Override
	public void onSuspend(AtmosphereResourceEvent event) {

	}

	@Override
	public void onResume(AtmosphereResourceEvent event) {

	}

	@Override
	public void onDisconnect(AtmosphereResourceEvent event) {
		cancel();
	}

	@Override
	public void onBroadcast(AtmosphereResourceEvent event) {

	}

	@Override
	public void onThrowable(AtmosphereResourceEvent event) {

	}

	@Override
	public void onClose(AtmosphereResourceEvent event) {
		cancel();
	}
}
