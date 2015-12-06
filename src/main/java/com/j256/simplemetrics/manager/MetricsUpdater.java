package com.j256.simplemetrics.manager;

/**
 * Implemented by those classes which need to update their states right before they are written out to the log files.
 * 
 * @author graywatson
 */
public interface MetricsUpdater {

	/**
	 * Called by {@link MetricsManager} right before it is to dump metrics out to file.
	 */
	public void updateMetrics();
}
