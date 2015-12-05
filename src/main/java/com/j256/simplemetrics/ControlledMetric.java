package com.j256.simplemetrics;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxSelfNaming;

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
public abstract class ControlledMetric<V, MV extends MetricValue<V, MV>> implements JmxSelfNaming,
		Comparable<ControlledMetric<V, MV>> {

	private final String component;
	private final String module;
	private final String name;
	private final String decription;
	private final String unit;

	private final MV initialValue = createInitialValue();

	private final AtomicReference<MV> metricValue = new AtomicReference<MV>(initialValue);

	protected ControlledMetric(String component, String module, String name, String description, String unit) {
		if (name == null) {
			throw new NullPointerException("Name cannot be null");
		}
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Name cannot be an empty or blank string");
		}
		if (component == null) {
			throw new NullPointerException("Component cannot be null");
		}
		if (StringUtils.isBlank(component)) {
			throw new IllegalArgumentException("Component cannot be an empty or blank string");
		}
		this.name = name;
		this.component = component;
		if (StringUtils.isBlank(module)) {
			this.module = null;
		} else {
			this.module = module;
		}
		if (StringUtils.isBlank(description)) {
			this.decription = null;
		} else {
			this.decription = description;
		}
		this.unit = unit;
	}

	/**
	 * Create the initial value for our metric.
	 */
	protected abstract MV createInitialValue();

	/**
	 * Make a value type from long input. This is only needed to support the {@link #adjustValue(long)} method.
	 */
	protected abstract V makeValueFromLong(long value);

	/**
	 * Make a value type from Number input. This is only needed to support the {@link #adjustValue(Number)} method.
	 */
	protected abstract V makeValueFromNumber(Number number);

	/**
	 * Returns the type of aggregation used by this metric.
	 */
	protected abstract AggregationType getAggregationType();

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
	@JmxAttributeMethod(description = "Current value of metric.")
	public Number getValue() {
		return getMetricValue(false).getNumber();
	}

	/**
	 * Return the value details of the metric.
	 */
	public MetricValueDetails getValueDetails() {
		return new MetricValueDetails(getMetricValue(false));
	}

	/**
	 * Get the current number for persisting purposes. This causes the metrics to be set to reset on next adjustment.
	 */
	public Number getValueToPersist() {
		return getMetricValue(true).getNumber();
	}

	/**
	 * Get the current value details for persisting purposes. This causes the metrics to be set to reset on next
	 * adjustment.
	 */
	public MetricValueDetails getValueDetailsToPersist() {
		return new MetricValueDetails(getMetricValue(true));
	}

	@JmxAttributeMethod(description = "Aggregation type for dealing with multiple entries")
	public String getAggregationTypeName() {
		return getAggregationType().name();
	}

	/**
	 * Adjust the value by a long primitive value. If this is an accumulator then it will be added. If this is a value
	 * then it will bet set.
	 */
	public void adjustValue(long value) {
		storeValue(makeValueFromLong(value));
	}

	/**
	 * Adjust the metric with this value. If this is an accumulator then it will be added. If this is a value then it
	 * will bet set.
	 */
	public void adjustValue(Number value) {
		storeValue(makeValueFromNumber(value));
	}

	/**
	 * Reset the counter to the initial value.
	 */
	public void reset() {
		metricValue.set(initialValue);
	}

	@Override
	public String getJmxDomainName() {
		return "anet.core";
	}

	@Override
	public String getJmxBeanName() {
		return name;
	}

	@Override
	public JmxFolderName[] getJmxFolderNames() {
		return new JmxFolderName[] { new JmxFolderName("metrics"), new JmxFolderName(component) };
	}

	@JmxAttributeMethod(description = "Metric name")
	public String getName() {
		return name;
	}

	@JmxAttributeMethod(description = "Metric component")
	public String getComponent() {
		return component;
	}

	@JmxAttributeMethod(description = "Metric module")
	public String getModule() {
		return module;
	}

	@JmxAttributeMethod(description = "Metric description")
	public String getDescription() {
		return decription;
	}

	@JmxAttributeMethod(description = "Metric unit [optional].")
	public String getUnit() {
		return unit;
	}

	@Override
	public int compareTo(ControlledMetric<V, MV> metric) {
		int compare = component.compareTo(metric.component);
		if (compare == 0) {
			return name.compareTo(metric.name);
		} else {
			return compare;
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(component).append(name).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || (!(obj instanceof ControlledMetric))) {
			return false;
		}
		ControlledMetric<?, ?> other = (ControlledMetric<?, ?>) obj;
		return (other.component.equals(component) && other.name.equals(name));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(component);
		if (module != null) {
			sb.append(':').append(module);
		}
		sb.append(':').append(name);
		return sb.toString();
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

	/**
	 * When we need to aggregate multiple values of this metric, this determines how we do so.
	 */
	public enum AggregationType {
		/** multiple values (such as page-view counts) are summed together */
		SUM,
		/** multiple values (such as disk free space) are averaged together */
		AVERAGE,
		// end
		;
	}
}
