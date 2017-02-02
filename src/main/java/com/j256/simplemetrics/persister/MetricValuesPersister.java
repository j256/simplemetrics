package com.j256.simplemetrics.persister;

import java.io.IOException;
import java.util.Map;

import com.j256.simplemetrics.metric.ControlledMetric;

/**
 * Class which publishes our metrics to disk, a cloud service, or to some other repository. This most likely is going to
 * need to be implemented by you unless the built in persisters (which are basically implementation examples) are really
 * all you need. This persisters just numbers as opposed to {@link MetricDetailsPersister} which persists more
 * information.
 * 
 * @author graywatson
 */
public interface MetricValuesPersister {

	/**
	 * Persists the metrics parameters to disk or some repository.
	 * 
	 * @param metricValues
	 *            The collection of metric and metric-values we are persisting.
	 * @param timeCollectedMillis
	 *            The time in millis when the metrics were collected.
	 * @throws IOException
	 *             If there was an i/o error while persisting.
	 */
	public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeCollectedMillis) throws IOException;
}
