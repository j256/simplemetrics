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
import com.j256.simplemetrics.utils.FileMetricsPublisher;
import com.j256.simplemetrics.utils.FileMetric.ProcMetricKind;

public class FileMetricsPublisherTest {

	private static final String PROC_PREFIX = "target/test-classes/proc";

	@Test
	public void testStuff() {
		String label = "foo";
		FileMetric metric =
				new FileMetric(label, "comp", null, "desc", new File(PROC_PREFIX, "meminfo"),
						ProcMetricKind.COLUMN_VALUE, 1, " +", "Cached:");

		MetricsManager manager = new MetricsManager();
		new FileMetricsPublisher(manager, Arrays.asList(metric));

		manager.updateMetrics();
		Map<ControlledMetric<?, ?>, Number> metricsValueMap = manager.getMetricValuesMap();

		Number value = metricsValueMap.get(metric.getMetric());
		assertNotNull(value);
		// it's a constant from the file
		assertEquals(4385684, value.longValue());
	}
}
