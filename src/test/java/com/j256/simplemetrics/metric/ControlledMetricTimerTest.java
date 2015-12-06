package com.j256.simplemetrics.metric;

import java.util.Random;

import org.junit.Test;

import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.metric.ControlledMetricTimer;

public class ControlledMetricTimerTest {

	private static final int MIN_DELAY = 50;
	private static final int MAX_DELAY = 200;

	@Test(timeout = 10000)
	public void testControlledMetricTimer() throws Exception {
		// we loop here until it works to avoid race conditions
		long avg;
		long diff;
		do {
			ControlledMetricTimer timer = new ControlledMetricTimer(this.getClass().getName(), null, "test", "desc");
			Random random = new Random();
			int NUM_TIMES = 5;
			int delaySum = 0;
			for (int i = 0; i < NUM_TIMES; i++) {
				int delay = MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY);
				long id = timer.start();
				Thread.sleep(delay);
				timer.stop(id);
				delaySum += delay;
			}

			long value = timer.getValue().longValue();
			avg = delaySum / NUM_TIMES;
			diff = Math.abs(value - avg);
		} while (diff >= (avg / 2));
	}

	@Test
	public void testControlledMetricTimerRegister() {
		MetricsManager manager = new MetricsManager();
		JmxServer jmxServer = new JmxServer();
		manager.setJmxServer(jmxServer);
		ControlledMetricTimer timer = new ControlledMetricTimer(this.getClass().getName(), null, "test", "desc");
		manager.registerMetric(timer);
	}
}
