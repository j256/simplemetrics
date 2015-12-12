package com.j256.simplemetrics.example;

import java.io.IOException;
import java.util.Random;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.persister.MetricValuesPersister;
import com.j256.simplemetrics.persister.MetricsPersisterJob;
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
		// create a simple metrics persister that writes to System.out
		SystemOutMetricsPersister metricsPersister = new SystemOutMetricsPersister();
		manager.setMetricValuesPersisters(new MetricValuesPersister[] { metricsPersister });

		// instantiate a couple of metrics
		ControlledMetricAccum hitsMetric =
				new ControlledMetricAccum("example", null, "hits", "number of hits to the cache", null);
		ControlledMetricAccum missesMetric =
				new ControlledMetricAccum("example", null, "misses", "number of misses to the cache", null);

		// register them with the manager
		manager.registerMetric(hitsMetric);
		manager.registerMetric(missesMetric);

		// start up the persisting thread to persist the metrics every so often
		// this is persisting every 1 second but you'll probably want to do a minute (60000) or something
		MetricsPersisterJob persisterThread = new MetricsPersisterJob(manager, 1000, 1000, true);

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
