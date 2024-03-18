package com.dca.context;

import java.util.HashSet;
import java.util.Set;

import com.dca.contribution.Contribution;
import com.dca.contribution.ContributionHandler;
import com.dca.handler.NoiseObjectListenerHandler;

public class NoiseContextImpl implements NoiseContext {

	private final String name;
	private final Set<ContributionHandler<?>> contributionHandlers = new HashSet<ContributionHandler<?>>();
	private final NoiseObjectListenerHandler nolHandler = new NoiseObjectListenerHandler();
	private final Set<Contribution> contributions = new HashSet<Contribution>();

	public NoiseContextImpl(String name) {
		this.name = name;
		this.addContributionHandler(nolHandler);
	}
	
	@Override
	public synchronized <T extends Contribution> void addContributionHandler(final ContributionHandler<T> contributionHandler) {
		contributionHandlers.add(contributionHandler);
		for (final T e : getContributions(contributionHandler.getType())) {
			safelyExecute(new Runnable() {
				@Override
				public void run() {
					contributionHandler.addContribution(e);
				}
			});
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T extends Contribution> Set<T> getContributions(Class<T> type) {
		Set<T> result = new HashSet<T>();
		for (Contribution extension : contributions) {
			if (type.isAssignableFrom(extension.getClass())) {
				result.add((T) extension);
			}
		}
		return result;
	}
	
	private void safelyExecute(Runnable r) {
		// try {
		r.run();
		// } catch (Throwable e) {
		// getLogger().log(Level.SEVERE,
		// "Can not register extension at extension consumer");
		// }
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public synchronized boolean hasContribution(Contribution contribution) {
		return contributions.contains(contribution);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <U> void addContribution(final Contribution contribution) {
		if (contributions.contains(contribution)) {
			throw new IllegalArgumentException(
					"Extension is already added ('" + contribution.getClass().getSimpleName() + "')");
		}

		contributions.add(contribution);
		for (@SuppressWarnings("rawtypes")
		final ContributionHandler ec : contributionHandlers) {
			if (ec.getType().isAssignableFrom(contribution.getClass())) {
				safelyExecute(new Runnable() {
					@Override
					public void run() {
						ec.addContribution(contribution);
					}
				});
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void removeContribution(final Contribution contribution) {
		if (!contributions.contains(contribution)) {
			throw new IllegalArgumentException("Extension is not contained");
		}

		for (@SuppressWarnings("rawtypes")
		final ContributionHandler ec : contributionHandlers) {
			if (ec.getType().isAssignableFrom(contribution.getClass())) {
				safelyExecute(new Runnable() {
					@Override
					public void run() {
						ec.removeContribution(contribution);
					}
				});
			}
		}
		contributions.remove(contribution);
	}

	@Override
	public synchronized <T extends Contribution> void removeContributionHandler(final ContributionHandler<T> extensionHandler) {
		for (final T e : getContributions(extensionHandler.getType())) {
			safelyExecute(new Runnable() {
				@Override
				public void run() {
					extensionHandler.removeContribution(e);
				}
			});
		}
	}
}
