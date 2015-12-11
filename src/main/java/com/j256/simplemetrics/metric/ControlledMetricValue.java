package com.j256.simplemetrics.metric;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.manager.MetricsUpdater;
import com.j256.simplemetrics.metric.ControlledMetricValue.ValueCount;

/**
 * Managed {@link ControlledMetric} for metrics like number of threads running or the JVM memory usage. It is used where
 * you are reseting it each time as opposed to a {@link ControlledMetricAccum}. If you need to poll a system property or
 * other object value then you may want your class to implement {@link MetricsUpdater}.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" }, description = "Controlled Value")
public class ControlledMetricValue extends BaseControlledMetric<Double, ValueCount> {

	/**
	 * @param component
	 *            Component short name such as "my".
	 * @param module
	 *            Module name to identify the part of the component such as "pageview".
	 * @param name
	 *            String label description the metric.
	 * @param description
	 *            Description for more information which may not be persisted.
	 * @param unit
	 *            Unit of the metric.
	 */
	public ControlledMetricValue(String component, String module, String name, String description, String unit) {
		super(component, module, name, description, unit);
	}

	@Override
	public ValueCount createInitialValue() {
		return new ValueCount(0.0, 0, 0.0, 0.0, true);
	}

	@Override
	public Double makeValueFromLong(long value) {
		return (double) value;
	}

	@Override
	public Double makeValueFromNumber(Number value) {
		return value.doubleValue();
	}

	@Override
	public AggregationType getAggregationType() {
		return AggregationType.AVERAGE;
	}

	/**
	 * Wrapper around a current value and count so we can calculate averages internally.
	 */
	public static class ValueCount implements MetricValue<Double, ValueCount> {
		private final double value;
		private final int count;
		private final double min;
		private final double max;
		private final boolean resetNext;

		private ValueCount(double value, int count, double min, double max, boolean resetNext) {
			this.value = value;
			this.count = count;
			this.min = min;
			this.max = max;
			this.resetNext = resetNext;
		}

		@Override
		public ValueCount makeResetNext() {
			return new ValueCount(value, count, min, max, true);
		}

		@Override
		public ValueCount makeAdjusted(Double value) {
			if (resetNext) {
				return new ValueCount(value, 1, value, value, false);
			}
			double min = this.min;
			double max = this.max;
			if (value < min) {
				min = value;
			} else if (value > max) {
				max = value;
			}
			// NOTE: we are adding in the value because we will be averaging later divided by count
			return new ValueCount(this.value + value, this.count + 1, min, max, false);
		}

		@Override
		public boolean isResetNext() {
			return resetNext;
		}

		@Override
		public Number getValue() {
			double doubleValue = value;
			if (count > 1) {
				// value is an _average_ of all the adjustments
				doubleValue /= count;
			}
			return Double.valueOf(doubleValue);
		}

		@Override
		public int getNumSamples() {
			return count;
		}

		@Override
		public Number getMin() {
			return Double.valueOf(min);
		}

		@Override
		public Number getMax() {
			return Double.valueOf(max);
		}
	}
}
