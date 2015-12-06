package com.j256.simplemetrics.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.manager.MetricsUpdater;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;
import com.j256.simplemetrics.persister.MetricsPersister;

public class MetricsManagerTest implements MetricsUpdater {

	private int pollCount = 0;

	@Test
	public void testRegisterGet() {
		MetricsManager manager = new MetricsManager();
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		assertEquals(1, manager.getMetrics().size());
		assertTrue(manager.getMetrics().contains(metric));
		manager.unregisterMetric(metric);
	}

	@Test
	public void testCallback() throws Exception {
		MetricsManager manager = new MetricsManager();
		int before = pollCount;
		manager.registerUpdater(this);
		manager.setMetricsPersisters(new MetricsPersister[0]);
		manager.persist();
		assertEquals(before + 1, pollCount);
	}

	@Test
	public void testPersister() throws Exception {
		MetricsManager manager = new MetricsManager();
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);

		TestPersister persister = new TestPersister();
		manager.setMetricsPersisters(new MetricsPersister[] { persister });

		assertNull(persister.lastValueMap);
		manager.persist();
		assertNotNull(persister.lastValueMap);

		Number value = persister.lastValueMap.get(metric);
		assertNotNull(value);

		assertTrue(value.longValue() == 0);
		long val = 10;
		metric.add(val);

		manager.persist();
		assertNotNull(persister.lastValueMap);
		value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertTrue(value.longValue() == val);
	}

	@Test
	public void testCoverage() {
		MetricsManager manager = new MetricsManager();
		manager.setJmxServer(new JmxServer());
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		manager.persistJmx();
		manager.getMetricValues();
	}

	@Override
	public void updateMetrics() {
		pollCount++;
	}

	private static class TestPersister implements MetricsPersister {
		Map<ControlledMetric<?, ?>, Number> lastValueMap;

		@Override
		public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeCollectedMillis) {
			lastValueMap = metricValues;
		}
	}
}
