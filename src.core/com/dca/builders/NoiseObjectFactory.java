package com.dca.builders;

import com.dca.concepts.NoiseObject;

public interface NoiseObjectFactory {
	
	public interface Callback {

		void onBuilt(NoiseObject object);

	}

	public boolean build(String type, Callback callback);
}
