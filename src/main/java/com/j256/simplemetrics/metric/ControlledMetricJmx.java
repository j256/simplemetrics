package com.j256.simplemetrics.metric;

import java.util.ArrayList;
import java.util.List;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplemetrics.manager.MetricsManagerJmx;

/**
 * Wrapper around a ControlledMetric that provides JMX publishing of the metric. This is used by the
 * {@link MetricsManagerJmx} if the optional SimpleJmx library is available.
 * 
 * @author graywatson
 */
public class ControlledMetricJmx implements JmxSelfNaming {

	private final ControlledMetric<?, ?> metric;
	private final String jmxDomainName;
	private final JmxFolderName[] jmxFolderNames;

	public ControlledMetricJmx(ControlledMetric<?, ?> metric, String jmxDomainName,
			JmxFolderName[] managerFolderNames) {
		this.metric = metric;
		this.jmxDomainName = jmxDomainName;
		this.jmxFolderNames = extractFolderNames(metric, managerFolderNames);
	}

	@JmxAttributeMethod(description = "Current value of metric.")
	public Number getValue() {
		return metric.getValue();
	}

	@JmxAttributeMethod(description = "Aggregation type for dealing with multiple entries")
	public String getAggregationTypeName() {
		return metric.getAggregationTypeName();
	}

	@JmxAttributeMethod(description = "Metric component")
	public String getComponent() {
		return metric.getComponent();
	}

	@JmxAttributeMethod(description = "Metric module")
	public String getModule() {
		return metric.getModule();
	}

	@JmxAttributeMethod(description = "Metric name")
	public String getName() {
		return metric.getName();
	}

	@JmxAttributeMethod(description = "Metric description")
	public String getDescription() {
		return metric.getDescription();
	}

	@JmxAttributeMethod(description = "Metric unit [optional].")
	public String getUnit() {
		return metric.getUnit();
	}

	@Override
	public String getJmxBeanName() {
		return metric.getName();
	}

	@Override
	public String getJmxDomainName() {
		return jmxDomainName;
	}

	@Override
	public JmxFolderName[] getJmxFolderNames() {
		return jmxFolderNames;
	}

	private JmxFolderName[] extractFolderNames(ControlledMetric<?, ?> metric, JmxFolderName[] managerFolderNames) {
		List<JmxFolderName> folderNames = new ArrayList<>(managerFolderNames.length + 2);
		for (JmxFolderName folderName : managerFolderNames) {
			folderNames.add(folderName);
		}
		folderNames.add(new JmxFolderName(metric.getComponent()));
		if (metric.getModule() != null) {
			folderNames.add(new JmxFolderName(metric.getModule()));
		}
		return folderNames.toArray(new JmxFolderName[folderNames.size()]);
	}
}
