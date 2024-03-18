package com.dca.context;

import java.util.Set;

import com.dca.contribution.Contribution;
import com.dca.contribution.ContributionHandler;

public interface NoiseContext {

	public String getName();

	public boolean hasContribution(Contribution extension);

	public <U> void addContribution(Contribution extension);

	public <T extends Contribution> Set<T> getContributions(Class<T> clazz);

	public void removeContribution(Contribution extension);

	public <T extends Contribution> void addContributionHandler(ContributionHandler<T> extensionHandler);

	public <T extends Contribution> void removeContributionHandler(ContributionHandler<T> extensionHandler);
	

}
