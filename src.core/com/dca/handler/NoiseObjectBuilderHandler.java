package com.dca.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dca.builders.NoiseObjectBuilder;
import com.dca.builders.NoiseObjectFactory;
import com.dca.concepts.NoiseObject;
import com.dca.contribution.ContributionHandler;

public final class NoiseObjectBuilderHandler
	implements ContributionHandler<NoiseObjectBuilder>, NoiseObjectFactory {

		private final List<NoiseObjectBuilder> builders = new ArrayList<NoiseObjectBuilder>();
		private final Map<String, List<Callback>> openTypes = new HashMap<String, List<Callback>>();

		@Override
		public synchronized void addContribution(NoiseObjectBuilder e) {
			builders.add(e);
			String[] types = e.getProvidedTypes();
			for (String type : types) {
				if (openTypes.containsKey(type)) {
					for (Callback callback : new ArrayList<Callback>(openTypes.get(type))) {
						if (build(type, e, callback)) {
							openTypes.get(type).remove(callback);
						}
					}
					if (openTypes.get(type).isEmpty()) {
						openTypes.remove(type);
					}
				}
			}
		}

		@Override
		public synchronized void removeContribution(NoiseObjectBuilder e) {
			builders.remove(e);
		}

		@Override
		public Class<NoiseObjectBuilder> getType() {
			return NoiseObjectBuilder.class;
		}

		@Override
		public synchronized boolean build(String type, Callback callback) {
			for (NoiseObjectBuilder builder : this.builders) {
				if (builder.canBuild(type)) {
					if (build(type, builder, callback)) {
						return true;
					}
				}
			}

			if (!openTypes.containsKey(type)) {
				openTypes.put(type, new ArrayList<Callback>());
			}
			openTypes.get(type).add(callback);
			return false;
		}

		private boolean build(String type, NoiseObjectBuilder builder, Callback callback) {
			NoiseObject object;
			try {
				object = builder.build(type);
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}
			try {
				callback.onBuilt(object);
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return true;
		}

	}
