package com.j256.simplemetrics.manager;

import java.io.IOException;

import javax.management.JMException;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricJmx;

/**
 * Class which optionally handles the JMX publishing of all of the metrics as JMX beans as well as the metrics-manager.
 *
 * @author graywatson
 */
@JmxResource(beanName = "MetricsManager", description = "Metrics Manager")
public class MetricsManagerJmx implements MetricsRegisterListener, JmxSelfNaming {

	private MetricsManager metricsManager;
	private JmxServer jmxServer;
	private String jmxDomainName = "com.j256";
	private JmxFolderName[] jmxFolderNames = { new JmxFolderName("metrics") };

	public MetricsManagerJmx() {
		// for spring
	}

	public MetricsManagerJmx(MetricsManager metricsManager, JmxServer jmxServer) {
		this.metricsManager = metricsManager;
		this.jmxServer = jmxServer;
	}

	@Override
	public void metricRegistered(ControlledMetric<?, ?> metric) {
		try {
			jmxServer.register(new ControlledMetricJmx(metric, jmxDomainName, jmxFolderNames));
		} catch (JMException e) {
			// ignored
		}
	}

	@Override
	public void metricUnregistered(ControlledMetric<?, ?> metric) {
		jmxServer.unregister(metric);
	}

	/**
	 * Update the various classes' metrics.
	 */
	@JmxOperation(description = "Update the various metrics")
	public void updateMetrics() {
		metricsManager.updateMetrics();
	}

	@JmxOperation(description = "Persist metrics using the registered persisters")
	public String persist() {
		try {
			metricsManager.persist();
			return "metrics published";
		} catch (IOException e) {
			return "Threw: " + e.getMessage();
		}
	}

	@JmxAttributeMethod(description = "Metric values we are managing")
	public String[] getMetricValues() {
		// update the metrics
		metricsManager.updateMetrics();
		return metricsManager.getMetricValues();
	}

	@JmxAttributeMethod(description = "Number of times we have persisted the metrics")
	public int getPersistCount() {
		return metricsManager.getPersistCount();
	}

	// @Required
	public void setMetricsManager(MetricsManager metricsManager) {
		this.metricsManager = metricsManager;
		this.metricsManager.registerRegisterListener(this);
	}

	/**
	 * Set the jmx-server that can be used to publish metrics to JMX.
	 */
	// @Required
	public void setJmxServer(JmxServer jmxServer) {
		this.jmxServer = jmxServer;
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
