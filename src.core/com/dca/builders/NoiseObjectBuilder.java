package com.dca.builders;

import com.dca.concepts.NoiseObject;
import com.dca.contribution.Contribution;

/**
 * Interface for extensions providing device implementations.
 */
public interface NoiseObjectBuilder extends Contribution {
	/**
	 * Returns true if a robotics object of the given type can be build by this
	 * robotics builder. This method must return true at least for all names
	 * provided by {@link RoboticsObjectBuilder#getProvidedTypes()}
	 * 
	 * @param type the type
	 * @return true if a robotics object can be build
	 */
	public boolean canBuild(String type);

	/**
	 * Instantiates a robotics object of the given type. This method must return
	 * valid robotics objects at least for all types where
	 * {@link RoboticsObjectBuilder#canBuild(String)} returns true.
	 * 
	 * @param type the type
	 * @return the robotics object
	 */
	public NoiseObject build(String type);

	/**
	 * Returns a set of provided types. For all types,
	 * {@link RoboticsObjectBuilder#canBuild(String)} returns true and
	 * {@link RoboticsObjectBuilder#build(String)} returns a valid robotics object.
	 * 
	 * @return a set of provided types
	 */
	public String[] getProvidedTypes();

}
