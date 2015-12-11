package com.j256.simplemetrics.persister;

import java.io.IOException;
import java.util.Map;

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.MetricValueDetails;

/**
 * Class which publishes our metrics to disk, a cloud service, or to some other repository. This most likely is going to
 * need to be implemented by you unless the built in persisters (which are basically implementation examples) are really
 * all you need. This persisters metric details in {@link MetricValueDetails} as opposed to
 * {@link MetricValuesPersister} which persists just numbers.
 * 
 * @author graywatson
 */
public interface MetricDetailsPersister {

	/**
	 * Persists the metrics parameters to disk or some repository.
	 * 
	 * @param metricValueDetails
	 *            The collection of metric and values-details we are persisting.
	 * 
	 * @param timeCollectedMillis
	 *            The time in millis when the metrics were collected.
	 */
	public void persist(Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails, long timeCollectedMillis)
			throws IOException;
}
