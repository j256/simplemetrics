package com.j256.simplemetrics.examples;

import java.io.IOException;
import java.util.Random;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.manager.MetricsPersisterThread;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.persister.MetricsPersister;
import com.j256.simplemetrics.persister.SystemOutMetricsPersister;

/**
 * Basic example which shows some of the features of the SimpleMetrics package.
 * 
 * @author graywatson
 */
public class BasicExample {

	public static void main(String[] args) throws IOException {
		// instantiate our manager
		MetricsManager manager = new MetricsManager();

		// instantiate a couple of metrics
		ControlledMetricAccum hitsMetric =
				new ControlledMetricAccum("example", null, "hits", "number of hits to the cache", null);
		ControlledMetricAccum missesMetric =
				new ControlledMetricAccum("example", null, "misses", "number of misses to the cache", null);

		// register them with the manager
		manager.registerMetric(hitsMetric);
		manager.registerMetric(missesMetric);

		// create a simple metrics persister that writes to System.out
		SystemOutMetricsPersister metricsPersister = new SystemOutMetricsPersister();
		manager.setMetricsPersisters(new MetricsPersister[] { metricsPersister });

		// start up the persisting thread to persist the metrics every so often
		MetricsPersisterThread persisterThread = new MetricsPersisterThread(manager, 1000, 1000, true);

		// now we run our application which is just doing some random counting
		Random random = new Random();
		for (long i = 0; i < 1000000000L; i++) {
			// ok, we don't have a cache so we'll simulate using random
			if (random.nextBoolean()) {
				hitsMetric.increment();
			} else {
				missesMetric.increment();
			}
		}
		// persist at the very end in case your computer is faster than mine (or its the future)
		manager.persist();

		// shutdown the persister thread
		persisterThread.destroy();
	}
}
