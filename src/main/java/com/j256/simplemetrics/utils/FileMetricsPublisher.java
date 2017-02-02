package com.j256.simplemetrics.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.manager.MetricsUpdater;

/**
 * Exposes metrics that can be found from files on file-system. This is often used to read from the /proc file-system
 * under Linux. Metrics are configured with the {@link FileMetric} class. A common file metric is the number of open
 * file-descriptors being used by the JVM.
 * 
 * <p>
 * <b>NOTE:</b> If you are using the no-arg constructor (like with Spring) you will need to make sure that
 * {@link #initialize()} is called.
 * </p>
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" },
		description = "Publisher that reads the file system to publish metrics")
public class FileMetricsPublisher implements MetricsUpdater {

	private MetricsManager metricsManager;
	private List<FileMetric> fileMetrics;

	private final AtomicLong failedUpdateCount = new AtomicLong(0);

	public FileMetricsPublisher() {
		// for spring
	}

	/**
	 * Constructs our publisher and calls {@link #initialize()}.
	 */
	public FileMetricsPublisher(MetricsManager metricsManager, List<FileMetric> fileMetrics) {
		this.metricsManager = metricsManager;
		this.fileMetrics = fileMetrics;
		initialize();
	}

	/**
	 * Should be called if the no-arg construct is being used and after the various setters have been called. Maybe by
	 * Springs init mechanism?
	 */
	public void initialize() {
		for (FileMetric metric : fileMetrics) {
			metricsManager.registerMetric(metric.getMetric());
		}
		this.metricsManager.registerUpdater(this);
	}

	/**
	 * Metrics manager that we add our metrics to. 
	 */
	// @Required
	public void setMetricsManager(MetricsManager metricsManager) {
		this.metricsManager = metricsManager;
	}

	/**
	 * Sets the file metrics that we are publishing. 
	 */
	// @Required
	public void setFileMetrics(FileMetric[] fileMetrics) {
		this.fileMetrics = new ArrayList<FileMetric>(fileMetrics.length);
		// only add those that are enabled
		for (FileMetric metric : fileMetrics) {
			if (metric.isInitialized()) {
				this.fileMetrics.add(metric);
			}
		}
	}

	@Override
	@JmxOperation(description = "Read in the values of the file metrics")
	public void updateMetrics() {
		for (FileMetric fileMetric : fileMetrics) {
			try {
				fileMetric.updateValue();
			} catch (IOException e) {
				failedUpdateCount.incrementAndGet();
			}
		}
	}

	/**
	 * Number of times we were unable to update a metric because of some i/o or parse problem. 
	 */
	@JmxAttributeMethod(description = "Number of failed updates")
	public long getFailedUpdateCount() {
		return failedUpdateCount.get();
	}

	/**
	 * Get the values for all of the metric managed by this class. 
	 */
	@JmxAttributeMethod(description = "Values of configured file metrics")
	public String[] getMetricsValues() {
		List<String> results = new ArrayList<String>();
		for (FileMetric fileMetric : fileMetrics) {
			results.add(fileMetric.getMetric().toString() + " = " + fileMetric.getMetric().getValue());
		}
		return results.toArray(new String[results.size()]);
	}
}
