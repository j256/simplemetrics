package com.j256.simplemetrics.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.JMException;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.MetricValueDetails;
import com.j256.simplemetrics.persister.MetricDetailsPersister;
import com.j256.simplemetrics.persister.MetricValuesPersister;

/**
 * Class which manages the various metrics that are in the system so they can be queried by operations. You register
 * metrics with this class, register classes that need to manually update metrics values, and controls the metrics
 * persistence.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" }, description = "Metrics Manager")
public class MetricsManager {

	private JmxServer jmxServer;
	private MetricValuesPersister[] metricValuesPersisters = new MetricValuesPersister[0];
	private MetricDetailsPersister[] metricDetailsPersisters = new MetricDetailsPersister[0];

	private final List<ControlledMetric<?, ?>> metrics = new ArrayList<ControlledMetric<?, ?>>();
	private final List<MetricsUpdater> metricsUpdaters = new ArrayList<MetricsUpdater>();
	private int persistCount;

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
	 * Persists the configured metrics by calling to the registered updaters, extracting the value-details from the
	 * metrics, and then calling the registered persisters.
	 */
	public void persist() throws IOException {

		// update the metric values if necessary
		updateMetrics();

		// if we aren't persisting details then this is easy
		if (metricDetailsPersisters.length == 0) {
			persistValues();
			return;
		}

		// first we make a unmodifiable map of metric -> details for the persisters
		long timeCollectedMillis = System.currentTimeMillis();
		Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails;
		synchronized (metrics) {
			metricValueDetails = new HashMap<ControlledMetric<?, ?>, MetricValueDetails>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				metricValueDetails.put(metric, metric.getValueDetailsToPersist());
			}
		}

		// if we have value persisters then extract the values from the details map
		Map<ControlledMetric<?, ?>, Number> metricValues = null;
		if (metricValuesPersisters.length > 0) {
			metricValues = new HashMap<ControlledMetric<?, ?>, Number>(metricValueDetails.size());
			for (Entry<ControlledMetric<?, ?>, MetricValueDetails> entry : metricValueDetails.entrySet()) {
				metricValues.put(entry.getKey(), entry.getValue().getValue());
			}
			metricValues = Collections.unmodifiableMap(metricValues);
		}
		metricValueDetails = Collections.unmodifiableMap(metricValueDetails);

		Exception wasThrown = null;
		for (MetricValuesPersister persister : metricValuesPersisters) {
			try {
				persister.persist(metricValues, timeCollectedMillis);
			} catch (Exception e) {
				// hold any exceptions thrown by them so we can get through all persisters
				wasThrown = e;
			}
		}
		for (MetricDetailsPersister persister : metricDetailsPersisters) {
			try {
				persister.persist(metricValueDetails, timeCollectedMillis);
			} catch (Exception e) {
				// hold any exceptions thrown by them so we can get through all persisters
				wasThrown = e;
			}
		}
		persistCount++;
		if (wasThrown != null) {
			if (wasThrown instanceof IOException) {
				throw (IOException) wasThrown;
			} else {
				throw new IOException(wasThrown);
			}
		}
	}

	/**
	 * Persists the configured metrics by extracting the values from the metrics and then calling the registered
	 * persisters.
	 * 
	 * NOTE: you should call updateMetrics() before calling persist if necessary.
	 */
	public void persistValues() throws IOException {

		// first we make a unmodifiable map of metric -> persisted value for the persisters
		long timeCollectedMillis = System.currentTimeMillis();
		Map<ControlledMetric<?, ?>, Number> metricValues;
		synchronized (metrics) {
			metricValues = new HashMap<ControlledMetric<?, ?>, Number>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				metricValues.put(metric, metric.getValueToPersist());
			}
		}
		metricValues = Collections.unmodifiableMap(metricValues);

		Exception wasThrown = null;
		for (MetricValuesPersister persister : metricValuesPersisters) {
			try {
				persister.persist(metricValues, timeCollectedMillis);
			} catch (Exception e) {
				// hold any exceptions thrown by them so we can get through all persisters
				wasThrown = e;
			}
		}
		persistCount++;
		if (wasThrown != null) {
			if (wasThrown instanceof IOException) {
				throw (IOException) wasThrown;
			} else {
				throw new IOException(wasThrown);
			}
		}
	}

	/**
	 * Persists the configured metrics by extracting the values from the metrics and then calling the registered
	 * persisters.
	 * 
	 * NOTE: you should call updateMetrics() before calling persist if necessary.
	 */
	public void persistValueDetails() throws IOException {

		// first we make a unmodifiable map of metric -> persisted value for the persisters
		long timeCollectedMillis = System.currentTimeMillis();
		Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails;
		synchronized (metrics) {
			metricValueDetails = new HashMap<ControlledMetric<?, ?>, MetricValueDetails>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				metricValueDetails.put(metric, metric.getValueDetailsToPersist());
			}
		}
		metricValueDetails = Collections.unmodifiableMap(metricValueDetails);

		Exception wasThrown = null;
		for (MetricDetailsPersister persister : metricDetailsPersisters) {
			try {
				persister.persist(metricValueDetails, timeCollectedMillis);
			} catch (Exception e) {
				// hold any exceptions thrown by them so we can get through all persisters
				wasThrown = e;
			}
		}
		persistCount++;
		if (wasThrown != null) {
			if (wasThrown instanceof IOException) {
				throw (IOException) wasThrown;
			} else {
				throw new IOException(wasThrown);
			}
		}
	}

	/**
	 * Return a map of the controlled metrics and their current associated values.
	 * 
	 * NOTE: this does not call {@link #updateMetrics()} beforehand.
	 */
	public Map<ControlledMetric<?, ?>, Number> getMetricValuesMap() {
		synchronized (metrics) {
			Map<ControlledMetric<?, ?>, Number> metricValues =
					new HashMap<ControlledMetric<?, ?>, Number>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				Number value = metric.getValue();
				// convert the value to a long if possible
				if (value.doubleValue() == value.longValue()) {
					value = value.longValue();
				}
				metricValues.put(metric, value);
			}
			return metricValues;
		}
	}

	/**
	 * Return a map of the controlled metrics and their current associated values.
	 * 
	 * NOTE: this does not call {@link #updateMetrics()} beforehand.
	 */
	public Map<ControlledMetric<?, ?>, MetricValueDetails> getMetricValueDetailsMap() {
		synchronized (metrics) {
			Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails =
					new HashMap<ControlledMetric<?, ?>, MetricValueDetails>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				metricValueDetails.put(metric, metric.getValueDetails());
			}
			return metricValueDetails;
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
			persistValues();
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

	// @NotRequired("Default is none")
	public void setJmxServer(JmxServer jmxServer) {
		this.jmxServer = jmxServer;
	}

	// @NotRequired("Default is a value or value-details persister")
	public void setMetricValuesPersisters(MetricValuesPersister[] metricValuesPersisters) {
		this.metricValuesPersisters = metricValuesPersisters;
	}

	// @NotRequired("Default is a value or value-details persister")
	public void setMetricDetailsPersisters(MetricDetailsPersister[] metricDetailsPersisters) {
		this.metricDetailsPersisters = metricDetailsPersisters;
	}

	@JmxAttributeMethod(description = "Metric values we are managing")
	public String[] getMetricValues() {
		// update the metrics
		updateMetrics();
		List<String> values;
		synchronized (metrics) {
			values = new ArrayList<String>(metrics.size());
			for (ControlledMetric<?, ?> metric : metrics) {
				values.add(metric + "=" + metric.getValue());
			}
		}
		return values.toArray(new String[values.size()]);
	}

	@JmxAttributeMethod(description = "Number of times we have persisted the metrics")
	public int getPersistCount() {
		return persistCount;
	}
}
