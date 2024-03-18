package com.dca;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class ConceptLibrary {

	
	private static Map<String, Constructor<? extends ConceptBuilder>> typeMap = new HashMap<>();
	
	static {
		try {
			typeMap.put("Source", AbstractSourceConcept.Builder.class.getConstructor(Map.class));
			typeMap.put("Engine", AbstractEngineConcept.Builder.class.getConstructor(Map.class));
		} catch (NoSuchMethodException | SecurityException e) {
			
			e.printStackTrace();
		}
	}

	public static Constructor<? extends ConceptBuilder> getConstructor(String string) {
		return typeMap.get(string);
	}
}
