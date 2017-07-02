package com.j256.simplemetrics.manager;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import javax.management.JMException;

import org.easymock.EasyMock;
import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;
import com.j256.simplemetrics.metric.MetricValueDetails;
import com.j256.simplemetrics.persister.MetricDetailsPersister;
import com.j256.simplemetrics.persister.MetricValuesPersister;

public class MetricsManagerTest {

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
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "name", "desc", null);
		manager.registerMetric(metric);
		assertEquals(1, manager.getMetrics().size());
		long val = 12321321321L;
		metric.adjustValue(val);
		Map<ControlledMetric<?, ?>, Number> valueMap = manager.getMetricValuesMap();
		Number value = valueMap.get(metric);
		assertNotNull(value);
		assertEquals(val, value.longValue());

		ControlledMetricValue metric2 = new ControlledMetricValue("comp", "mod", "name2", "desc", null);
		manager.registerMetric(metric2);

		double val2 = 12.32;
		metric2.adjustValue(val2);
		valueMap = manager.getMetricValuesMap();
		value = valueMap.get(metric2);
		assertNotNull(value);
		assertEquals(val2, value.doubleValue(), 0);
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
		LocalMetricsUpdater updater = new LocalMetricsUpdater();
		int before = updater.pollCount;
		manager.registerUpdater(updater);
		manager.persist();
		assertEquals(before + 1, updater.pollCount);
	}

	@Test
	public void testValuesPersister() throws Exception {
		MetricsManager manager = new MetricsManager();
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);

		TestValuesPersister persister = new TestValuesPersister();
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { persister });

		assertNull(persister.lastValueMap);
		manager.persistValuesOnly();
		assertNotNull(persister.lastValueMap);

		Number value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(0, value.longValue());

		long val = 10;
		metric.add(val);

		manager.persistValuesOnly();
		assertNotNull(persister.lastValueMap);
		value = persister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(val, value.longValue());
	}

	@Test
	public void testValuesAndDetailsPersister() throws Exception {
		MetricsManager manager = new MetricsManager();
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);

		TestValuesPersister valuesPersister = new TestValuesPersister();
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { valuesPersister });

		TestDetailsPersister detailsPersister = new TestDetailsPersister();
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { detailsPersister });

		assertNull(valuesPersister.lastValueMap);
		assertNull(detailsPersister.lastValueMap);

		double doubleVal = 1.0;
		metric.adjustValue(doubleVal);

		manager.persist();

		assertNotNull(valuesPersister.lastValueMap);
		assertNotNull(detailsPersister.lastValueMap);
		Number value = detailsPersister.lastValueMap.get(metric).getValue();
		assertNotNull(value);

		value = detailsPersister.lastValueMap.get(metric).getValue();
		assertNotNull(value);
		assertEquals((long) doubleVal, value);

		value = valuesPersister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(1, value.longValue());

		long val = 10;
		metric.add(val);

		manager.persist();
		assertNotNull(valuesPersister.lastValueMap);
		value = valuesPersister.lastValueMap.get(metric);
		assertNotNull(value);
		assertEquals(val, value.longValue());

	}

	@Test
	public void testCoverage() throws JMException {
		MetricsManager manager = new MetricsManager();
		JmxServer jmxServer = new JmxServer(9157);
		try {
			jmxServer.start();
			ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
			manager.registerMetric(metric);
			manager.unregisterMetric(metric);
			manager.setJmxServer(jmxServer);
			manager.registerMetric(metric);
			manager.getMetricValues();
			manager.persistJmx();
			manager.unregisterMetric(metric);
			manager.unregisterMetric(metric);
		} finally {
			jmxServer.stop();
		}
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

	@Test(expected = IOException.class)
	public void testDetailsPersisterThrows() throws IOException {
		MetricsManager manager = new MetricsManager();
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		MetricDetailsPersister detailsPersister = EasyMock.createMock(MetricDetailsPersister.class);

		detailsPersister.persist(EasyMock.<Map<ControlledMetric<?, ?>, MetricValueDetails>> anyObject(), anyLong());
		expectLastCall().andThrow(new RuntimeException());

		replay(detailsPersister);
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { detailsPersister });
		manager.persist();
		verify(detailsPersister);
	}

	@Test(expected = IOException.class)
	public void testValuePersisterThrows() throws IOException {
		MetricsManager manager = new MetricsManager();
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		metric.adjustValue(100);
		MetricValuesPersister valuesPersister = EasyMock.createMock(MetricValuesPersister.class);

		valuesPersister.persist(EasyMock.<Map<ControlledMetric<?, ?>, Number>> anyObject(), anyLong());
		expectLastCall().andThrow(new RuntimeException());

		replay(valuesPersister);
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { valuesPersister });
		manager.persist();
		verify(valuesPersister);
	}

	@Test(expected = IOException.class)
	public void testBothPersisterThrows() throws IOException {
		MetricsManager manager = new MetricsManager();
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "label", "desc", null);
		manager.registerMetric(metric);
		metric.adjustValue(100);
		MetricDetailsPersister detailsPersister = EasyMock.createMock(MetricDetailsPersister.class);
		MetricValuesPersister valuesPersister = EasyMock.createMock(MetricValuesPersister.class);

		detailsPersister.persist(EasyMock.<Map<ControlledMetric<?, ?>, MetricValueDetails>> anyObject(), anyLong());
		expectLastCall().andThrow(new RuntimeException()).anyTimes();
		valuesPersister.persist(EasyMock.<Map<ControlledMetric<?, ?>, Number>> anyObject(), anyLong());
		expectLastCall().andThrow(new RuntimeException()).anyTimes();

		replay(detailsPersister, valuesPersister);
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { detailsPersister });
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { valuesPersister });
		assertEquals(0, manager.getPersistCount());
		assertTrue(manager.persistJmx().contains("Threw: "));
		assertEquals(1, manager.getPersistCount());
		manager.persist();
		verify(detailsPersister, valuesPersister);
	}

	private static class LocalMetricsUpdater implements MetricsUpdater {

		int pollCount = 0;

		@Override
		public void updateMetrics() {
			pollCount++;
		}
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
		public void persist(Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails,
				long timeCollectedMillis) {
			lastValueMap = metricValueDetails;
		}
	}
}
