package com.j256.simplemetrics.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.utils.FileMetric;
import com.j256.simplemetrics.utils.FileMetric.FileMetricKind;
import com.j256.simplemetrics.utils.FileMetricsPublisher;

public class FileMetricsPublisherTest {

	private static final String PROC_PREFIX = "target/test-classes/proc";

	@Test
	public void testStuff() {
		String label = "foo";
		FileMetric metric = new FileMetric(label, "comp", null, "desc", new File(PROC_PREFIX, "meminfo"),
				FileMetricKind.FILE_VALUE, 1, " +", "Cached:");

		MetricsManager manager = new MetricsManager();
		new FileMetricsPublisher(manager, Arrays.asList(metric));

		manager.updateMetrics();
		Map<ControlledMetric<?, ?>, Number> metricsValueMap = manager.getMetricValuesMap();

		Number value = metricsValueMap.get(metric.getMetric());
		assertNotNull(value);
		// it's a constant from the file
		assertEquals(4385684, value.longValue());
	}

	@Test
	public void testDir() {
		String label = "foo";
		FileMetric metric = new FileMetric(label, "self", null, "fds", new File(PROC_PREFIX, "self/fd"),
				FileMetricKind.DIR, 0, " ", null);

		MetricsManager manager = new MetricsManager();
		FileMetricsPublisher publisher = new FileMetricsPublisher();
		publisher.setMetricsManager(manager);
		publisher.setFileMetrics(new FileMetric[] { metric });
		publisher.initialize();

		manager.updateMetrics();
		Map<ControlledMetric<?, ?>, Number> metricsValueMap = manager.getMetricValuesMap();

		Number value = metricsValueMap.get(metric.getMetric());
		assertNotNull(value);
		// it's a constant from the file
		assertEquals(4, value.longValue());
		assertEquals(0, publisher.getFailedUpdateCount());
		String[] values = publisher.getMetricsValues();
		assertNotNull(values);
		assertEquals(1, values.length);
		assertEquals("self.foo = 4.0", values[0]);
	}
}
