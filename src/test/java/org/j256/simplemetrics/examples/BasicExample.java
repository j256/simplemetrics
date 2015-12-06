package org.j256.simplemetrics.examples;

import java.io.IOException;
import java.util.Random;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.manager.MetricsPersisterThread;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.persister.MetricsPersister;
import com.j256.simplemetrics.persister.SystemOutMetricsPersister;

/**
 * Basic example which shows some of the featurs of the SimpleMetrics package.
 * 
 * @author graywatson
 */
public class BasicExample {

	public static void main(String[] args) throws IOException {
		// instantiate our manager
		MetricsManager manager = new MetricsManager();

		// construct our metrics
		ControlledMetricAccum hitsMetric =
				new ControlledMetricAccum("example", null, "hits", "number of hits to the cache", null);
		ControlledMetricAccum missesMetric =
				new ControlledMetricAccum("example", null, "misses", "number of misses to the cache", null);

		// register them with the manager
		manager.registerMetric(hitsMetric);
		manager.registerMetric(missesMetric);

		// create a simple metrics persisterO
		SystemOutMetricsPersister metricsPersister = new SystemOutMetricsPersister();
		manager.setMetricsPersisters(new MetricsPersister[] { metricsPersister });

		// start up the persisting thread to persist the metrics every so often
		MetricsPersisterThread persisterThread = new MetricsPersisterThread(manager, 1000, 1000, true);

		Random random = new Random();
		for (long i = 0; i < 1000000000L; i++) {
			if (random.nextBoolean()) {
				hitsMetric.increment();
			} else {
				missesMetric.increment();
			}
		}
		// persist at the end
		manager.persist();

		// shutdown the persister thread
		persisterThread.destroy();
	}
}
