package com.j256.simplemetrics;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.ControlledMetricValue.ValueCount;

/**
 * Managed {@link ControlledMetric} for metrics like Thread-Count or page response times where you are reseting it each
 * time as opposed to a {@link ControlledMetricAccum}.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "anet.core", description = "Controlled Value")
public class ControlledMetricValue extends ControlledMetric<Double, ValueCount> {

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
	protected ValueCount createInitialValue() {
		return new ValueCount(0.0, 0, 0.0, 0.0, true);
	}

	@Override
	protected Double makeValueFromLong(long value) {
		return (double) value;
	}

	@Override
	protected Double makeValueFromNumber(Number value) {
		return value.doubleValue();
	}

	@Override
	protected AggregationType getAggregationType() {
		return AggregationType.AVERAGE;
	}

	/**
	 * Wrapper around a current value and count so we can calculate averages internally.
	 */
	public static class ValueCount implements MetricValue<Double, ValueCount> {
		private final double value;
		private final int count;
		private final boolean resetNext;
		private final double min;
		private final double max;

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
			} else {
				double min;
				double max;
				if (value < this.min) {
					min = value;
					max = this.max;
				} else if (value > this.max) {
					min = this.min;
					max = value;
				} else {
					min = this.min;
					max = this.max;
				}
				// NOTE: we are adding in the value because we will be averaging later divided by count
				return new ValueCount(this.value + value, this.count + 1, min, max, false);
			}
		}

		@Override
		public boolean isResetNext() {
			return resetNext;
		}

		@Override
		public Number getNumber() {
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
