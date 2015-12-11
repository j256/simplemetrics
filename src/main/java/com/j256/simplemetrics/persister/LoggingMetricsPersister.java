package com.j256.simplemetrics.persister;

import java.util.Map;
import java.util.logging.Logger;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Publishes metrics to the java.util.Logger class using the {@link Logger#info(String)} method.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" }, description = "Text File Metrics Persister")
public class LoggingMetricsPersister implements MetricValuesPersister {

	private static final Logger logger = Logger.getLogger(LoggingMetricsPersister.class.getSimpleName());

	@Override
	public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeMillis) {
		for (Map.Entry<ControlledMetric<?, ?>, Number> entry : metricValues.entrySet()) {
			ControlledMetric<?, ?> metric = entry.getKey();
			Number value = entry.getValue();
			logger.info(MiscUtils.metricToString(metric) + " = " + value);
		}
	}
}
