package com.j256.simplemetrics.metric;

import java.util.concurrent.atomic.AtomicLong;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.metric.ControlledMetricAccum.AccumValue;

/**
 * Managed {@link ControlledMetric} for metrics like page-count or database-accesses that you are adding to continually
 * as opposed to a {@link ControlledMetricValue}.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", description = "Controlled Accumulator")
public class ControlledMetricAccum extends ControlledMetric<Long, AccumValue> {

	// We have this intermediate counter because we want to not have every increment cause another metric value object
	private final AtomicLong counter = new AtomicLong();

	/**
	 * @param component
	 *            Component short name such as "my". Required.
	 * @param module
	 *            Module name to identify the part of the component such as "pageview". Null if none.
	 * @param name
	 *            String label description the metric. Required.
	 * @param description
	 *            Description for more information which may not be persisted.
	 * @param unit
	 *            Unit of the metric. Null if none.
	 */
	public ControlledMetricAccum(String component, String module, String name, String description, String unit) {
		super(component, module, name, description, unit);
	}

	@Override
	protected AccumValue createInitialValue() {
		return new AccumValue(0, true);
	}

	@Override
	protected Long makeValueFromLong(long value) {
		return Long.valueOf(value);
	}

	@Override
	protected Long makeValueFromNumber(Number value) {
		return value.longValue();
	}

	/**
	 * Add a delta value to the metric. This is for metrics (like pageview count) which are incrementing over time.
	 */
	public long add(long delta) {
		return counter.addAndGet(delta);
	}

	/**
	 * Add one to the metric.
	 */
	public long increment() {
		return counter.incrementAndGet();
	}

	@Override
	public void adjustValue(long value) {
		// we overload this so so we don't generate a new object on every adjustment
		counter.addAndGet(value);
	}

	@Override
	public void adjustValue(Number value) {
		// we overload this so so we don't generate a new object on every adjustment
		counter.addAndGet(value.longValue());
	}

	@Override
	public Number getValue() {
		long value = counter.getAndSet(0);
		if (value > 0) {
			// we adjust here only when the value is needed so we don't generate a new object on every adjustment
			super.adjustValue(value);
		}
		return super.getValue();
	}

	@Override
	public Number getValueToPersist() {
		long value = counter.getAndSet(0);
		if (value > 0) {
			// we adjust here only when the value is needed so we don't generate a new object on every adjustment
			super.adjustValue(value);
		}
		return super.getValueToPersist();
	}

	@Override
	protected AggregationType getAggregationType() {
		return AggregationType.SUM;
	}

	/**
	 * Wrapper around a counter long value and a reset flag.
	 */
	public static class AccumValue implements MetricValue<Long, AccumValue> {
		private final long value;
		private final boolean resetNext;

		private AccumValue(long value, boolean resetNext) {
			this.value = value;
			this.resetNext = resetNext;
		}

		@Override
		public AccumValue makeResetNext() {
			return new AccumValue(value, true);
		}

		@Override
		public AccumValue makeAdjusted(Long newValue) {
			if (resetNext) {
				return new AccumValue(newValue, false);
			} else {
				return new AccumValue(this.value + newValue, false);
			}
		}

		@Override
		public boolean isResetNext() {
			return resetNext;
		}

		@Override
		public Number getNumber() {
			return Long.valueOf(value);
		}

		@Override
		public int getNumSamples() {
			// not 100% sure this is right but the count is the number of samples.
			if (value >= Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			} else {
				return (int) value;
			}
		}

		@Override
		public Number getMin() {
			// with an accumulator, the min/max is just the count
			return Long.valueOf(value);
		}

		@Override
		public Number getMax() {
			// with an accumulator, the min/max is just the count
			return Long.valueOf(value);
		}
	}
}
