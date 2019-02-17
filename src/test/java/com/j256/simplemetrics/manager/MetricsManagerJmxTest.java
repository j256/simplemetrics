package com.j256.simplemetrics.manager;

import javax.management.JMException;

import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.metric.ControlledMetricAccum;

public class MetricsManagerJmxTest {

	@Test
	public void testStuff() throws JMException {
		MetricsManager manager = new MetricsManager();
		MetricsManagerJmx managerJmx = new MetricsManagerJmx();
		JmxServer jmxServer = new JmxServer(9157);
		managerJmx.setJmxServer(jmxServer);
		managerJmx.setMetricsManager(manager);
		try {
			jmxServer.start();
			ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", "label", "desc", null);
			manager.registerMetric(metric);
			manager.unregisterMetric(metric);
			manager.registerMetric(metric);
			managerJmx.getMetricValues();
			managerJmx.persist();
			manager.unregisterMetric(metric);
			manager.unregisterMetric(metric);
		} finally {
			jmxServer.stop();
		}
	}
}
