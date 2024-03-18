package com.dca.handler;

import java.util.ArrayList;
import java.util.List;

import com.dca.concepts.NoiseObject;
import com.dca.contribution.ContributionHandler;
import com.dca.listeners.NoiseObjectListener;

public class NoiseObjectListenerHandler implements ContributionHandler<NoiseObjectListener> {


	private final List<NoiseObjectListener> registeredListeners = new ArrayList<NoiseObjectListener>();
	private final List<NoiseObject> registeredObjects = new ArrayList<NoiseObject>();

	public NoiseObjectListenerHandler() {
	}

	@Override
	public Class<NoiseObjectListener> getType() {
		return NoiseObjectListener.class;
	}

	protected synchronized void onAvailable(final NoiseObject object) {
		registeredObjects.add(object);
		for (final NoiseObjectListener l : registeredListeners) {
			safelyExecute(new Runnable() {
				@Override
				public void run() {
					l.onAvailable(object);
				}
			});
		}
	}

	protected synchronized void onUnavailable(final NoiseObject object) {
		registeredObjects.remove(object);
		for (final NoiseObjectListener l : registeredListeners) {
			safelyExecute(new Runnable() {
				@Override
				public void run() {
					l.onUnavailable(object);
				}
			});
		}
	}

	@Override
	public synchronized void addContribution(final NoiseObjectListener l) {
		registeredListeners.add(l);
		for (final NoiseObject ro : registeredObjects) {
			safelyExecute(new Runnable() {
				@Override
				public void run() {
					l.onAvailable(ro);
				}
			});
		}
	}

	@Override
	public synchronized void removeContribution(final NoiseObjectListener l) {
		registeredListeners.remove(l);
		for (final NoiseObject ro : registeredObjects) {
			safelyExecute(new Runnable() {
				@Override
				public void run() {
					l.onUnavailable(ro);
				}
			});
		}
	}

	private void safelyExecute(Runnable r) {
		try {
			r.run();
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println("Error while notifying noise object listener");
		}
	}

}
