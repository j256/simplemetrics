package com.j256.simplemetrics.utils;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;

/**
 * JMX information for the file-metrics publisher.
 * 
 * @author graywatson
 */
@JmxResource(beanName = "FileMetricsPublisher", description = "Publisher that reads the file system to publish metrics")
public class FileMetricsPublisherJmx implements JmxSelfNaming {

	private FileMetricsPublisher metricsPublisher;
	private String jmxDomainName = "com.j256";
	private JmxFolderName[] jmxFolderNames = { new JmxFolderName("metrics") };

	public FileMetricsPublisherJmx() {
		// for spring
	}

	@JmxOperation(description = "Read in the values of the file metrics")
	public void updateMetrics() {
		metricsPublisher.updateMetrics();
	}

	@JmxAttributeMethod(description = "Number of failed updates")
	public long getFailedUpdateCount() {
		return metricsPublisher.getFailedUpdateCount();
	}

	@JmxAttributeMethod(description = "Values of configured file metrics")
	public String[] getMetricsValues() {
		return metricsPublisher.getMetricsValues();
	}

	/**
	 * Metrics manager that we add our metrics to.
	 */
	// @Required
	public void setMetricsPublisher(FileMetricsPublisher metricsPublisher) {
		this.metricsPublisher = metricsPublisher;
	}

	@Override
	public String getJmxBeanName() {
		// use the defined one above
		return null;
	}

	@Override
	public String getJmxDomainName() {
		return jmxDomainName;
	}

	/**
	 * Set the domain-name top-level folder for the metrics. Default is "com.j256".
	 */
	// @NotRequired("default is com.j256")
	public void setJmxDomainName(String jmxDomainName) {
		this.jmxDomainName = jmxDomainName;
	}

	@Override
	public JmxFolderName[] getJmxFolderNames() {
		return jmxFolderNames;
	}

	/**
	 * Set the sub-folder name for the metrics. Default is {"metrics"}.
	 */
	// @NotRequired("default is { \"metrics\" }")
	public void setJmxFolderNames(String[] folderNames) {
		JmxFolderName[] jmxFolderNames = new JmxFolderName[folderNames.length];
		for (int i = 0; i < jmxFolderNames.length; i++) {
			jmxFolderNames[i] = new JmxFolderName(folderNames[i]);
		}
		this.jmxFolderNames = jmxFolderNames;
	}
}
