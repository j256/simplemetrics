package org.j256.simplemetrics.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.manager.MetricsUpdater;
import com.j256.simplemetrics.metric.ControlledMetricValue;
import com.j256.simplemetrics.persister.MetricsPersister;

public class MetricsManagerTest implements MetricsUpdater {

	private int pollCount = 0;

	@Test
	public void testRegisterGet() {
		MetricsManager manager = new MetricsManager();
		manager.setJmxServer(new JmxServer());
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

	@Override
	public void updateMetrics() {
		pollCount++;
	}
}
