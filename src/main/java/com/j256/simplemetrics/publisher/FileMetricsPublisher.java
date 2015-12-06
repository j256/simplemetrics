package com.j256.simplemetrics.publisher;

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
 * Class that will expose some good metrics that can be found from files on file-system. Often used to read from the
 * /proc file-system under Linux.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" },
		description = "Publisher that reads the file system to publish metrics")
public class FileMetricsPublisher implements MetricsUpdater {

	private MetricsManager metricsManager;
	private List<FileMetric> fileMetrics;
	private AtomicLong failedUpdateCount = new AtomicLong(0);

	public FileMetricsPublisher() {
		// for spring
	}

	public FileMetricsPublisher(MetricsManager metricsManager, List<FileMetric> fileMetrics) {
		this.metricsManager = metricsManager;
		this.fileMetrics = fileMetrics;
		initialize();
	}

	/**
	 * Should be called after the file metrics have been set. Maybe by Springs init mechanism?
	 */
	public void initialize() {
		for (FileMetric metric : fileMetrics) {
			metricsManager.registerMetric(metric.getMetric());
		}
		this.metricsManager.registerUpdatePoll(this);
	}

	// @Required
	public void setMetricsManager(MetricsManager metricsManager) {
		this.metricsManager = metricsManager;
	}

	// @Required
	public void setFileMetrics(FileMetric[] fileMetrics) {
		this.fileMetrics = new ArrayList<FileMetric>(fileMetrics.length);
		// only add those that are enabled
		for (FileMetric metric : fileMetrics) {
			if (metric.isEnabled()) {
				this.fileMetrics.add(metric);
			}
		}
	}

	/**
	 * Update all of the file metrics.
	 */
	@Override
	@JmxOperation(description = "Poll the metrics files")
	public void updateMetrics() {
		for (FileMetric fileMetric : fileMetrics) {
			try {
				fileMetric.updateValue();
			} catch (IOException e) {
				failedUpdateCount.incrementAndGet();
			}
		}
	}

	@JmxAttributeMethod(description = "number of failed updates")
	public long getFailedUpdateCount() {
		return failedUpdateCount.get();
	}

	@JmxAttributeMethod(description = "File metrics")
	public String[] getFileMetricsJmx() {
		List<String> results = new ArrayList<String>();
		for (FileMetric fileMetric : fileMetrics) {
			results.add(fileMetric.getMetric().toString() + fileMetric.getMetric().getValue());
		}
		return results.toArray(new String[results.size()]);
	}
}
