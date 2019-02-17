package com.j256.simplemetrics.publisher;

import org.junit.Test;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.utils.SystemMetricsPublisher;

public class SystemMetricsPublisherTest {

	@Test
	public void testPollBeforeInit() {
		MetricsManager manager = new MetricsManager();
		SystemMetricsPublisher publisher = new SystemMetricsPublisher();
		publisher.setMetricsManager(manager);
		publisher.initialize();
		publisher.updateMetrics();
	}
}
