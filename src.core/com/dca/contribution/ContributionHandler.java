package com.dca.contribution;

public interface ContributionHandler <E extends Contribution> extends Contribution  {


	public void addContribution(E e);

	public void removeContribution(E e);

	public Class<E> getType();
}
