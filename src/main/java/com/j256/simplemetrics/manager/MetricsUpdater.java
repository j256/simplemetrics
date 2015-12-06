package com.j256.simplemetrics.manager;

/**
 * Implemented by those classes which need to update their states right before they are persisted. For example, if you
 * need to poll some other class to extract the metrics from it. You register an updater on the manager by calling
 * {@link MetricsManager#registerUpdater(MetricsUpdater)}.
 * 
 * @author graywatson
 */
public interface MetricsUpdater {

	/**
	 * Called by {@link MetricsManager} right before it is to persist the metrics.
	 */
	public void updateMetrics();
}
