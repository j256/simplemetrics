package com.j256.simplemetrics.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.persister.MetricsPersister;

/**
 * Class which manages the various metrics that are in the system so they can be queried by operations.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" }, description = "Metrics Manager")
public class MetricsManager {

	private JmxServer jmxServer;
	private MetricsPersister[] metricsPersisters = new MetricsPersister[0];

	private final List<ControlledMetric<?, ?>> metrics = new ArrayList<ControlledMetric<?, ?>>();
	private final List<MetricsUpdater> metricsUpdaters = new ArrayList<MetricsUpdater>();

	/**
	 * Register a metric with the manager.
	 */
	public void registerMetric(ControlledMetric<?, ?> metric) {
		synchronized (metrics) {
			if (metrics.add(metric) && jmxServer != null) {
				// register it with JMX if injected
				try {
					jmxServer.register(metric);
				} catch (JMException e) {
					// ignore it
				}
			}
		}
	}

	/**
	 * Unregister a metric with the manager.
	 */
	public void unregisterMetric(ControlledMetric<?, ?> metric) {
		synchronized (metrics) {
			if (metrics.remove(metric) && jmxServer != null) {
				jmxServer.unregister(metric);
			}
		}
	}

	/**
	 * Register a {@link MetricsUpdater} to be called right before persist writes the metrics.
	 */
	public void registerUpdater(MetricsUpdater metricsUpdater) {
		synchronized (metricsUpdaters) {
			metricsUpdaters.add(metricsUpdater);
		}
	}

	/**
	 * Persists the configured metrics by calling to the registered updaters, extracting the values from the metrics,
	 * and then calling the registered persisters.
	 */
	public void persist() throws IOException {

		// update the metrics
		updateMetrics();

		// first we make a unmodifiable map of metric -> persisted value for the persisters
		Map<ControlledMetric<?, ?>, Number> metricValues;
		synchronized (metrics) {
			metricValues = new HashMap<ControlledMetric<?, ?>, Number>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				metricValues.put(metric, metric.getValueToPersist());
			}
			metricValues = Collections.unmodifiableMap(metricValues);
		}

		long timeCollectedMillis = System.currentTimeMillis();
		Exception wasThrown = null;
		for (MetricsPersister persister : metricsPersisters) {
			try {
				persister.persist(metricValues, timeCollectedMillis);
			} catch (Exception e) {
				// hold any exceptions thrown by them so we can get through all persisters
				wasThrown = e;
			}
		}
		if (wasThrown != null) {
			if (wasThrown instanceof IOException) {
				throw (IOException) wasThrown;
			} else {
				throw new IOException(wasThrown);
			}
		}
	}

	/**
	 * Update the various classes' metrics.
	 */
	@JmxOperation(description = "Update the various metrics")
	public void updateMetrics() {
		synchronized (metricsUpdaters) {
			// call our classes to update their stats
			for (MetricsUpdater metricsUpdater : metricsUpdaters) {
				metricsUpdater.updateMetrics();
			}
		}
	}

	@JmxOperation(description = "Persist metrics using the registered persisters")
	public String persistJmx() {
		try {
			persist();
			return "metrics published";
		} catch (IOException e) {
			return "Threw: " + e.getMessage();
		}
	}

	/**
	 * @return An unmodifiable collection of metrics we are managing.
	 */
	public Collection<ControlledMetric<?, ?>> getMetrics() {
		synchronized (metrics) {
			return Collections.unmodifiableList(metrics);
		}
	}

	// @NotRequired("Default ius none")
	public void setJmxServer(JmxServer jmxServer) {
		this.jmxServer = jmxServer;
	}

	// @Required
	public void setMetricsPersisters(MetricsPersister[] metricsPersisters) {
		this.metricsPersisters = metricsPersisters;
	}

	@JmxAttributeMethod(description = "Metric values we are managing")
	public String[] getMetricValues() {
		// update the metrics
		updateMetrics();
		List<String> values = new ArrayList<String>(metrics.size());
		synchronized (metrics) {
			for (ControlledMetric<?, ?> metric : metrics) {
				values.add(metric.getComponent() + "." + metric.getName() + '=' + metric.getValue());
			}
		}
		return values.toArray(new String[values.size()]);
	}
}
