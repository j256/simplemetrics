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
		MetricsPersisterJob job = new MetricsPersisterJob();
		job.setMetricsManager(manager);
		job.setPeriodTimeMillis(millis);
		job.setDaemonThread(true);
		job.initialize();

		Thread.sleep(millis + millis / 10);
		assertEquals(1, manager.getPersistCount());

		job.destroyAndJoin();
	}

	@Test
	public void testTwice() throws Exception {
		MetricsManager manager = new MetricsManager();
		assertEquals(0, manager.getPersistCount());

		long millis = 100;
		MetricsPersisterJob job = new MetricsPersisterJob();
		job.setMetricsManager(manager);
		// coverage
		job.setDelayTimeMillis(0);
		job.setPeriodTimeMillis(millis);
		job.setDaemonThread(true);
		job.initialize();

		Thread.sleep(millis * 2 + millis / 10);
		assertEquals(3, manager.getPersistCount());

		job.destroyAndJoin();
	}

	@Test
	public void testCoverage() throws Exception {
		MetricsManager manager = new MetricsManager();
		assertEquals(0, manager.getPersistCount());

		long millis = 100;
		MetricsPersisterJob job = new MetricsPersisterJob();
		job.setMetricsManager(manager);
		job.setPeriodTimeMillis(millis);
		job.setDelayTimeMillis(0);
		job.setDaemonThread(true);
		job.initialize();

		Thread.sleep(millis * 2 + millis / 10);
		assertEquals(3, manager.getPersistCount());

		job.destroyAndJoin();
	}

	@Test
	public void testInterruptDelay() throws Exception {
		MetricsManager manager = new MetricsManager();
		assertEquals(0, manager.getPersistCount());

		long millis = 100;
		MetricsPersisterJob job = new MetricsPersisterJob(manager, millis * 2, millis, true);

		Thread.sleep(millis + millis / 10);

		job.destroyAndJoin();
		assertEquals(0, manager.getPersistCount());
	}
}
