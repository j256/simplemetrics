package org.j256.simplemetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.ControlledMetricValue;
import com.j256.simplemetrics.MetricsManager;
import com.j256.simplemetrics.MetricsPersister;
import com.j256.simplemetrics.MetricsUpdater;

public class MetricsManagerTest implements MetricsUpdater {

	private final static String TEMP_DIR = "target/metricsManagerTest";
	private int pollCount = 0;

	@Before
	@After
	public void cleanTmp() throws Exception {
		File tmpDir = new File(TEMP_DIR);
		tmpDir.delete();
		FileUtils.deleteDirectory(tmpDir);
	}

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
		manager.registerUpdatePoll(this);
		manager.setMetricsPersisters(new MetricsPersister[0]);
		manager.persist();
		assertEquals(before + 1, pollCount);
	}

	@Override
	public void updateMetrics() {
		pollCount++;
	}
}
