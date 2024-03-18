package com.dca;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import com.dca.context.NoiseContext;
import com.dca.context.NoiseContextImpl;
import com.dca.contribution.Contribution;
import com.dca.contribution.ContributionHandler;
import com.dca.handler.NoiseObjectBuilderHandler;

public class NoiseApi {

	private final NoiseContext context;
	private final NoiseObjectBuilderHandler builderHandler;
	
	private NoiseApi(String name) {

		this.context = new NoiseContextImpl(name);
		this.builderHandler = new NoiseObjectBuilderHandler();
		this.context.addContributionHandler(this.builderHandler);
	}
	
	
	
	public static NoiseApi createNewEmpty() {
		return createNewEmpty(null);
	}

	public static NoiseApi createNewEmpty(String name) {
		NoiseApi noiseApi = new NoiseApi(name);
		try {
			noiseApi.loadContributionsOverJspi();
		} catch (RuntimeException e) {
			throw e;
		}
		return noiseApi;
	}
	
	
	
	private void loadContributionsOverJspi() {
		List<Contribution> contributions = new ArrayList<>();
		List<ContributionHandler<?>> contributionHandlers = new ArrayList<>();
		for (Contribution extension : ServiceLoader.load(Contribution.class)) {
			contributions.add(extension);
			if (extension instanceof ContributionHandler) {
				contributionHandlers.add((ContributionHandler<?>) extension);
			}
		}
		contributionHandlers.forEach(h -> registerExtensionHandler(h));
		contributions.forEach(e -> registerContribution(e));
	}
	
	public final synchronized void registerContribution(Contribution extension) {
		context.addContribution(extension);
	}
	
	protected final synchronized <T extends Contribution> void registerExtensionHandler(
			ContributionHandler<T> extensionHandler) {
		context.addContributionHandler(extensionHandler);
	}
	
	
}
