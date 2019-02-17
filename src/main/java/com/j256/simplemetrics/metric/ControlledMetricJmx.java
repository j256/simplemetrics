package com.j256.simplemetrics.metric;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxSelfNaming;

/**
 * Wrapper around a ControlledMetric that provides JMX publishing of the metric.
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
		// add on the metrics component
		JmxFolderName[] metricFolderNames = new JmxFolderName[managerFolderNames.length + 1];
		System.arraycopy(managerFolderNames, 0, metricFolderNames, 0, managerFolderNames.length);
		metricFolderNames[managerFolderNames.length] = new JmxFolderName(metric.getComponent());
		this.jmxFolderNames = metricFolderNames;
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
}
