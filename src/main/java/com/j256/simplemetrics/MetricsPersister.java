package com.j256.simplemetrics;

import java.io.IOException;
import java.util.Map;

/**
 * Class which publishes a metrics to disk or some repository.
 * 
 * @author graywatson
 */
public interface MetricsPersister {

	/**
	 * Persists the metrics parameters to disk or some repository.
	 * 
	 * @param metricValues
	 *            The collection of metric and metric-values we are persisting.
	 * 
	 * @param timeMillis
	 *            The time in millis when the metrics were collected.
	 */
	public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeMillis) throws IOException;
}
