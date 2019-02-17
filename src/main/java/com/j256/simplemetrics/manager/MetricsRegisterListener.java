package com.j256.simplemetrics.manager;

import com.j256.simplemetrics.metric.ControlledMetric;

/**
 * Listener to get a callback when a metric has been registered and unregistered with the {@link MetricsManager}.
 * 
 * @author graywatson
 */
public interface MetricsRegisterListener {

	/**
	 * Metric has been registered with the manager.
	 */
	public void metricRegistered(ControlledMetric<?, ?> metric);

	/**
	 * Metric has been unregistered with the manager.
	 */
	public void metricUnregistered(ControlledMetric<?, ?> metric);
}
