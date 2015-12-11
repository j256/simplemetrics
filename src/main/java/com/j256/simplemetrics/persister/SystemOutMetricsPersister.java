package com.j256.simplemetrics.persister;

import java.util.Map;

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Very simple persister which dumps out metrics out to {@link System#out}. This is more here as a implementation
 * example than anything else.
 * 
 * @author graywatson
 */
public class SystemOutMetricsPersister implements MetricValuesPersister {

	@Override
	public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeCollectedMillis) {
		System.out.println("# persisting metrics to System.out");
		for (Map.Entry<ControlledMetric<?, ?>, Number> entry : metricValues.entrySet()) {
			System.out.println(MiscUtils.metricToString(entry.getKey()) + " = " + entry.getValue());
		}
	}
}
