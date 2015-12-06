package com.j256.simplemetrics.persister;

import java.io.IOException;
import java.util.Map;

import com.j256.simplemetrics.metric.ControlledMetric;

/**
 * Class which publishes a metrics to disk, a cloud service, or to some repository.
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
