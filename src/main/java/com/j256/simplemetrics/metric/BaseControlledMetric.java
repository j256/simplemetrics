package com.j256.simplemetrics.metric;

import java.util.concurrent.atomic.AtomicReference;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Base metric class which defines some common fields and methods.
 * 
 * @param <V>
 *            Value type that we use to adjust this metric-value.
 * @param <MV>
 *            Metric value which wraps and manages the raw value.
 * 
 * @author graywatson
 */
public abstract class BaseControlledMetric<V, MV extends MetricValue<V, MV>> implements ControlledMetric<V, MV>,
		JmxSelfNaming, Comparable<BaseControlledMetric<V, MV>> {

	private final String component;
	private final String module;
	private final String name;
	private final String decription;
	private final String unit;

	private final MV initialValue = createInitialValue();

	private final AtomicReference<MV> metricValue = new AtomicReference<MV>(initialValue);

	protected BaseControlledMetric(String component, String module, String name, String description, String unit) {
		if (name == null) {
			throw new NullPointerException("Name cannot be null");
		}
		if (MiscUtils.isBlank(name)) {
			throw new IllegalArgumentException("Name cannot be an empty or blank string");
		}
		if (component == null) {
			throw new NullPointerException("Component cannot be null");
		}
		if (MiscUtils.isBlank(component)) {
			throw new IllegalArgumentException("Component cannot be an empty or blank string");
		}
		this.name = name;
		this.component = component;
		if (MiscUtils.isBlank(module)) {
			this.module = null;
		} else {
			this.module = module;
		}
		if (MiscUtils.isBlank(description)) {
			this.decription = null;
		} else {
			this.decription = description;
		}
		this.unit = unit;
	}

	/**
	 * Stores the value into the metric.
	 */
	protected MV storeValue(V value) {
		MV currentVal;
		MV newVal;
		do {
			currentVal = metricValue.get();
			newVal = currentVal.makeAdjusted(value);
		} while (!metricValue.compareAndSet(currentVal, newVal));
		return newVal;
	}

	/**
	 * Return the number of the metric.
	 */
	@Override
	@JmxAttributeMethod(description = "Current value of metric.")
	public Number getValue() {
		return getMetricValue(false).getValue();
	}

	/**
	 * Return the value details of the metric.
	 */
	@Override
	public MetricValueDetails getValueDetails() {
		return new MetricValueDetails(getMetricValue(false));
	}

	/**
	 * Get the current number for persisting purposes. This causes the metrics to be set to reset on next adjustment.
	 */
	@Override
	public Number getValueToPersist() {
		Number value = getMetricValue(true).getValue();
		// see if we can return a long
		if (value.doubleValue() == value.longValue()) {
			return value.longValue();
		} else {
			return value;
		}
	}

	/**
	 * Get the current value details for persisting purposes. This causes the metrics to be set to reset on next
	 * adjustment.
	 */
	@Override
	public MetricValueDetails getValueDetailsToPersist() {
		return new MetricValueDetails(getMetricValue(true));
	}

	@Override
	@JmxAttributeMethod(description = "Aggregation type for dealing with multiple entries")
	public String getAggregationTypeName() {
		return getAggregationType().name();
	}

	@Override
	public void adjustValue(long value) {
		storeValue(makeValueFromLong(value));
	}

	@Override
	public void adjustValue(Number value) {
		storeValue(makeValueFromNumber(value));
	}

	@Override
	public String getJmxDomainName() {
		return "com.j256";
	}

	@Override
	public String getJmxBeanName() {
		return name;
	}

	@Override
	public JmxFolderName[] getJmxFolderNames() {
		return new JmxFolderName[] { new JmxFolderName("metrics"), new JmxFolderName(component) };
	}

	@Override
	@JmxAttributeMethod(description = "Metric component")
	public String getComponent() {
		return component;
	}

	@Override
	@JmxAttributeMethod(description = "Metric module")
	public String getModule() {
		return module;
	}

	@Override
	@JmxAttributeMethod(description = "Metric name")
	public String getName() {
		return name;
	}

	@Override
	@JmxAttributeMethod(description = "Metric description")
	public String getDescription() {
		return decription;
	}

	@Override
	@JmxAttributeMethod(description = "Metric unit [optional].")
	public String getUnit() {
		return unit;
	}

	@Override
	public int compareTo(BaseControlledMetric<V, MV> metric) {
		int compare = component.compareTo(metric.component);
		if (compare != 0) {
			return compare;
		}
		if (module != null) {
			compare = module.compareTo(metric.module);
			if (compare != 0) {
				return compare;
			}
		}
		return name.compareTo(metric.name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + component.hashCode();
		result = prime * result + ((module == null) ? 0 : module.hashCode());
		result = prime * result + name.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		BaseControlledMetric<?, ?> other = (BaseControlledMetric<?, ?>) obj;
		if (!component.equals(other.component)) {
			return false;
		}
		if (module == null) {
			if (other.module != null) {
				return false;
			}
		} else if (!module.equals(other.module)) {
			return false;
		}
		return name.equals(other.name);
	}

	@Override
	public String toString() {
		return MiscUtils.metricToString(this);
	}

	private MV getMetricValue(boolean resetNext) {
		if (!resetNext) {
			// if we are not persisting, then just get the current value
			return metricValue.get();
		}

		MV newMetricValue;
		MV currentMetricValue;
		do {
			currentMetricValue = metricValue.get();
			if (currentMetricValue.isResetNext()) {
				// this means that we have not adjusted the value since the last time we persisted
				newMetricValue = initialValue;
			} else {
				/*
				 * Next time we adjust the value, it will reset to 0. We do this so the metric itself retains its value
				 * until the next time it is set or persisted so it doesn't immediately drop to 0 or something after
				 * each persist which shows up in JMX or other direct monitoring.
				 */
				newMetricValue = currentMetricValue.makeResetNext();
			}
		} while (!metricValue.compareAndSet(currentMetricValue, newMetricValue));

		return newMetricValue;
	}
}
