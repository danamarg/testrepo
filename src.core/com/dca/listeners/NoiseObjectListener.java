package com.dca.listeners;

import com.dca.concepts.NoiseObject;
import com.dca.contribution.Contribution;

/**
 * Listener for the event that a {@link NoiseObject} becomes available or
 * unavailable.
 */
public interface NoiseObjectListener extends Contribution {

	/**
	 * Called when a {@link NoiseObject} becomes available for the listener.
	 * 
	 * @param runtime the newly available object.
	 */
	void onAvailable(NoiseObject object);

	/**
	 * Called when a {@link NoiseObject} becomes unavailable for the listener.
	 * 
	 * @param runtime the soon unavailable object.
	 */
	void onUnavailable(NoiseObject object);

}
