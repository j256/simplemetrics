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

	private final List<ControlledMetric<?, ?>> metrics = new ArrayList<ControlledMetric<?, ?>>();
	private final List<MetricsUpdater> metricsUpdaters = new ArrayList<MetricsUpdater>();
	private JmxServer jmxServer;
	private MetricsPersister[] metricsPersisters;

	/**
	 * Register a metric with the manager.
	 */
	public void registerMetric(ControlledMetric<?, ?> metric) {
		synchronized (metrics) {
			if (metrics.add(metric)) {
				// register it with JMX
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
			if (metrics.remove(metric)) {
				jmxServer.unregister(metric);
			}
		}
	}

	/**
	 * Register a {@link MetricsUpdater} to be called right before persist writes the metrics.
	 */
	public synchronized void registerUpdatePoll(MetricsUpdater updatePoll) {
		metricsUpdaters.add(updatePoll);
	}

	/**
	 * Persists the configured metrics.
	 */
	public synchronized void persist() throws IOException {
		long timeMillis = System.currentTimeMillis();
		updateMetrics();
		Exception wasThrown = null;
		// first we make a map of metric -> persisted value for the persisters
		Map<ControlledMetric<?, ?>, Number> metricValues = new HashMap<ControlledMetric<?, ?>, Number>(metrics.size());
		for (ControlledMetric<?, ?> metric : metrics) {
			metricValues.put(metric, metric.getValueToPersist());
		}
		metricValues = Collections.unmodifiableMap(metricValues);
		for (MetricsPersister persister : metricsPersisters) {
			try {
				persister.persist(metricValues, timeMillis);
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
	@JmxOperation(description = "Poll the various metrics to get their values")
	public void updateMetrics() {
		// call our classes to update their stats
		for (MetricsUpdater metricsUpdater : metricsUpdaters) {
			metricsUpdater.updateMetrics();
		}
	}

	@JmxOperation(description = "Persist metrics to disk or ...")
	public String persistJmx() {
		try {
			persist();
			return "metrics published";
		} catch (IOException e) {
			return "Threw: " + e.getMessage();
		}
	}

	/**
	 * @return The collection of metrics we are managing.
	 */
	public Collection<ControlledMetric<?, ?>> getMetrics() {
		return Collections.unmodifiableList(metrics);
	}

	// @Required
	public void setJmxServer(JmxServer jmxServer) {
		this.jmxServer = jmxServer;
	}

	// @Required
	public void setMetricsPersisters(MetricsPersister[] metricsPersisters) {
		this.metricsPersisters = metricsPersisters;
	}

	@JmxAttributeMethod(description = "Metrics we are managing")
	public String[] getMetricValues() {
		// update the metrics
		updateMetrics();
		Collection<ControlledMetric<?, ?>> metrics = getMetrics();
		List<String> values = new ArrayList<String>(metrics.size());
		for (ControlledMetric<?, ?> metric : metrics) {
			values.add(metric.getComponent() + "." + metric.getName() + '=' + metric.getValue());
		}
		return values.toArray(new String[values.size()]);
	}
}
