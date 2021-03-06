package com.j256.simplemetrics.persister;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricAccum;

public class JavaUtilLoggingMetricsPersisterTest {

	@Test
	public void testStuff() {
		LoggingMetricsPersister persister = new LoggingMetricsPersister();
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
		Map<ControlledMetric<?, ?>, Number> metricValues = new HashMap<ControlledMetric<?, ?>, Number>();
		long value = 123123123213L;
		metricValues.put(metric, value);
		persister.persist(metricValues, System.currentTimeMillis());
	}
}
