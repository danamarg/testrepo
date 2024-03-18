package com.dca.file.simplesax;


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.xml.sax.SAXException;

import com.dca.file.DocumentLoadingContext;
import com.dca.file.warnings.BugException;

/**
 * A handler that populates the parameters of a previously constructed rocket component.
 * This uses the setters, or delegates the handling to another handler for specific
 * elements.
 */
class NoiseConceptHandler extends AbstractElementHandler {
	private final DocumentLoadingContext context;
	private final RocketComponent parent;

	public NoiseConceptHandler(RocketComponent c, DocumentLoadingContext context) {
		this.parent = c;
		this.context = context;

		// Sometimes setting certain component parameters will clear the preset. We don't want that to happen, so
		// ignore preset clearing.
		this.parent.setIgnorePresetClearing(true);
	}

	@Override
	public ElementHandler openElement(String element, HashMap<String, String> attributes) {

		// Attempt to get correct noise concept builder
		
		
		// Configure builder
		builder.configure(attributes, this.parent); // Pass in parent for context
		
		// Build Contribution
		
		
		// Create Corresponding Model Object
		
		
		
		
		
		
		// Attempt to construct new component
		Constructor<? extends RocketComponent> constructor = DocumentConfig.constructors
				.get(element);
		if (constructor == null) {
			System.err.println("Unknown element " + element + ", ignoring.");
			return null;
		}

		RocketComponent c;
		try {
			c = constructor.newInstance();
		} catch (InstantiationException e) {
			throw new BugException("Error constructing component.", e);
		} catch (IllegalAccessException e) {
			throw new BugException("Error constructing component.", e);
		} catch (InvocationTargetException e) {
			throw Reflection.handleWrappedException(e);
		}

		parent.addChild(c);

		return new ComponentParameterHandler(c, context);




		// Check for specific elements that contain other elements
		if (element.equals("subcomponents")) {
			return new ComponentHandler(component, context);
		}
		if ( element.equals("appearance")) {
			return new AppearanceHandler(component,context);
		}
		// TODO: delete 'inside-appearance' when backward compatibility with 22.02.beta.01-22.02.beta.05 is not needed anymore
		if (element.equals("insideappearance") || element.equals("inside-appearance")) {
			return new InsideAppearanceHandler(component, context);
		}
		if (element.equals("motormount")) {
			if (!(component instanceof MotorMount)) {
				warnings.add(Warning.fromString("Illegal component defined as motor mount."));
				return null;
			}
			return new MotorMountHandler((MotorMount) component, context);
		}
		if (element.equals("finpoints")) {
			if (!(component instanceof FreeformFinSet)) {
				warnings.add(Warning.fromString("Illegal component defined for fin points."));
				return null;
			}
			return new FinSetPointHandler((FreeformFinSet) component, context);
		}
		if (element.equals("motorconfiguration")) {
			if (!(component instanceof Rocket)) {
				warnings.add(Warning.fromString("Illegal component defined for motor configuration."));
				return null;
			}
			return new MotorConfigurationHandler((Rocket) component, context);
		}
		if (element.equals("flightconfiguration")) {
			if (!(component instanceof Rocket)) {
				warnings.add(Warning.fromString("Illegal component defined for flight configuration."));
				return null;
			}
			return new MotorConfigurationHandler((Rocket) component, context);
		}
		if ( element.equals("deploymentconfiguration")) {
			if ( !(component instanceof RecoveryDevice) ) {
				warnings.add(Warning.fromString("Illegal component defined as recovery device."));
				return null;
			}
			return new DeploymentConfigurationHandler( (RecoveryDevice) component, context );
		}
		if ( element.equals("separationconfiguration")) {
			if ( !(component instanceof AxialStage) ) {
				warnings.add(Warning.fromString("Illegal component defined as stage."));
				return null;
			}
			return new StageSeparationConfigurationHandler( (AxialStage) component, context );
		}

		return PlainTextHandler.INSTANCE;
	}

	@Override
	public void closeElement(String element, HashMap<String, String> attributes,
			String content) {

		// TODO: delete 'inside-appearance' when backward compatibility with 22.02.beta.01-22.02.beta.05 is not needed anymore
		if (element.equals("subcomponents") || element.equals("motormount") ||
				element.equals("finpoints") || element.equals("motorconfiguration") ||
				element.equals("appearance") || element.equals("insideappearance") || element.equals("inside-appearance") ||
				element.equals("deploymentconfiguration") || element.equals("separationconfiguration")) {
			return;
		}

		// Search for the correct setter class

		Class<?> c;
		for (c = component.getClass(); c != null; c = c.getSuperclass()) {
			String setterKey = c.getSimpleName() + ":" + element;
			Setter s = DocumentConfig.setters.get(setterKey);
			if (s != null) {
				// Setter found
				s.set(component, content, attributes);
				break;
			}
			if (DocumentConfig.setters.containsKey(setterKey)) {
				// Key exists but is null -> invalid parameter
				c = null;
				break;
			}
		}
		if (c == null) {
			System.err.println("Unknown parameter type '" + element + "' for "
					+ component.getComponentName() + ", ignoring.");
		}
	}

	@Override
	public void endHandler(String element, HashMap<String, String> attributes, String content) throws SAXException {
		super.endHandler(element, attributes, content, warnings);

		// Restore the preset clearing behavior
		this.component.setIgnorePresetClearing(false);
	}
}
