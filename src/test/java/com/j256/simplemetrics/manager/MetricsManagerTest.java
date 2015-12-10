package com.j256.simplemetrics.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;
import com.j256.simplemetrics.metric.MetricValueDetails;
import com.j256.simplemetrics.persister.MetricDetailsPersister;
import com.j256.simplemetrics.persister.MetricValuesPersister;

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
	public void testMetricValueMap() {
		MetricsManager manager = new MetricsManager();
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		assertEquals(1, manager.getMetrics().size());
		long val = 12321321321L;
		metric.adjustValue(val);
		Map<ControlledMetric<?, ?>, Number> valueMap = manager.getMetricValuesMap();
		Number value = valueMap.get(metric);
		assertNotNull(value);
		assertEquals(val, value.longValue());
		manager.unregisterMetric(metric);
	}

	@Test
	public void testMetricValueDetailsMap() {
		MetricsManager manager = new MetricsManager();
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		assertEquals(1, manager.getMetrics().size());
		long val = 12321321321L;
		metric.adjustValue(val);
		Map<ControlledMetric<?, ?>, MetricValueDetails> valueDetailsMap = manager.getMetricValueDetailsMap();
		MetricValueDetails details = valueDetailsMap.get(metric);
		assertNotNull(details);
		assertEquals(1, details.getNumSamples());
		assertEquals(val, details.getValue().longValue());
		assertEquals(val, details.getMin().longValue());
		assertEquals(val, details.getMax().longValue());
		manager.unregisterMetric(metric);
	}

	@Test
	public void testMetricsUpdater() throws IOException {
		MetricsManager manager = new MetricsManager();
		int before = pollCount;
		manager.registerUpdater(this);
		manager.persist();
		assertEquals(before + 1, pollCount);
	}

	@Test
	public void testPersister() throws Exception {
		MetricsManager manager = new MetricsManager();
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);

		TestValuesPersister persister = new TestValuesPersister();
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { persister });

		assertNull(persister.lastValueMap);
		manager.persistValues();
		assertNotNull(persister.lastValueMap);

		Number value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(0, value.longValue());

		long val = 10;
		metric.add(val);

		manager.persistValues();
		assertNotNull(persister.lastValueMap);
		value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(val, value.longValue());
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

	@Test
	public void testLongVersusDouble() throws IOException {
		MetricsManager manager = new MetricsManager();
		manager.setJmxServer(new JmxServer());
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		TestValuesPersister persister = new TestValuesPersister();
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { persister });

		double doubleVal = 1.1;
		metric.adjustValue(doubleVal);
		manager.persist();

		assertNotNull(persister.lastValueMap);
		Number value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(doubleVal, value);

		doubleVal = 1.0;
		metric.adjustValue(doubleVal);
		manager.persist();

		value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals((long) doubleVal, value);
	}

	@Test
	public void testLongVersusDoubleDetails() throws IOException {
		MetricsManager manager = new MetricsManager();
		manager.setJmxServer(new JmxServer());
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		TestDetailsPersister persister = new TestDetailsPersister();
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { persister });

		double doubleVal = 1.1;
		metric.adjustValue(doubleVal);
		manager.persist();

		assertNotNull(persister.lastValueMap);
		Number value = persister.lastValueMap.get(metric).getValue();
		assertNotNull(value);
		assertEquals(doubleVal, value);

		doubleVal = 1.0;
		metric.adjustValue(doubleVal);
		manager.persist();

		value = persister.lastValueMap.get(metric).getValue();
		assertNotNull(value);
		assertEquals((long) doubleVal, value);
	}

	@Override
	public void updateMetrics() {
		pollCount++;
	}

	private static class TestValuesPersister implements MetricValuesPersister {
		Map<ControlledMetric<?, ?>, Number> lastValueMap;

		@Override
		public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeCollectedMillis) {
			lastValueMap = metricValues;
		}
	}

	private static class TestDetailsPersister implements MetricDetailsPersister {
		Map<ControlledMetric<?, ?>, MetricValueDetails> lastValueMap;

		@Override
		public void persist(Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails, long timeCollectedMillis) {
			lastValueMap = metricValueDetails;
		}
	}
}
