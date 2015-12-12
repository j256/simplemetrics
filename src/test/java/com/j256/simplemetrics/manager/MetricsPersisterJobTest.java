package com.j256.simplemetrics.manager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.j256.simplemetrics.persister.MetricsPersisterJob;

public class MetricsPersisterJobTest {

	@Test
	public void testStuff() throws Exception {
		MetricsManager manager = new MetricsManager();
		assertEquals(0, manager.getPersistCount());

		long millis = 100;
		MetricsPersisterJob job = new MetricsPersisterJob(manager, millis, millis, true);

		Thread.sleep(millis + millis / 10);
		assertEquals(1, manager.getPersistCount());

		job.join();
	}
}
