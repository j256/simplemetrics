package com.j256.simplemetrics.metric;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.metric.ControlledMetricRatio.NumeratorDenominator;
import com.j256.simplemetrics.metric.ControlledMetricRatio.RatioValue;

/**
 * A metric which tracks the relationship between two values. For example, if you want to account for a cache hit ratio
 * or the average number of results per query. We manage this as a numerator and a denominator to ensure we don't lose
 * value resolution.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" },
		description = "Controlled metric ratio between two values")
public class ControlledMetricRatio extends BaseControlledMetric<NumeratorDenominator, RatioValue> {

	/**
	 * @param component
	 *            Component short name such as "web".
	 * @param module
	 *            Module name to identify the part of the component such as "pageview".
	 * @param name
	 *            String label description the metric.
	 * @param description
	 *            Description for more information which may not be persisted.
	 * @param unit
	 *            Unit of the metric.
	 */
	public ControlledMetricRatio(String component, String module, String name, String description, String unit) {
		super(component, module, name, description, unit);
	}

	@Override
	public RatioValue createInitialValue() {
		return new RatioValue(0.0, 0.0, 0, 0.0, 0.0, true);
	}

	@Override
	public NumeratorDenominator makeValueFromLong(long value) {
		return new NumeratorDenominator(value, 1);
	}

	@Override
	public NumeratorDenominator makeValueFromNumber(Number number) {
		return new NumeratorDenominator(number.doubleValue(), 1);
	}

	@Override
	public AggregationType getAggregationType() {
		return AggregationType.AVERAGE;
	}

	/**
	 * Adjust the value of our numerator and denominator to set new values for it. These will be added to the running
	 * values for the numerator and denominator. For example, if you want to account for a cache miss (i.e. a request
	 * and a miss) then you would say adjustValue(0, 1). Or if you are counting the number of results per query you
	 * might say adjustValue(3, 1).
	 * 
	 * NOTE: if you just want to set the value of this ratio and not have it be a running total, then you should
	 * consider just using a {@link ControlledMetricValue}.
	 */
	public void adjustValue(Number numerator, Number denominator) {
		storeValue(new NumeratorDenominator(numerator.doubleValue(), denominator.doubleValue()));
	}

	/**
	 * Class which holds a numerator and denominator double.
	 */
	public static class NumeratorDenominator {
		final double numerator;
		final double denominator;

		public NumeratorDenominator(double numerator, double denominator) {
			this.numerator = numerator;
			this.denominator = denominator;
		}
	}

	/**
	 * Wrapper around a numerator and denominator.
	 */
	public static class RatioValue implements MetricValue<NumeratorDenominator, RatioValue> {
		private final double numerator;
		private final double denominator;
		private int count;
		private final boolean resetNext;
		private final double min;
		private final double max;

		private RatioValue(double numerator, double denominator, int count, double min, double max, boolean resetNext) {
			this.numerator = numerator;
			this.denominator = denominator;
			this.count = count;
			this.min = min;
			this.max = max;
			this.resetNext = resetNext;
		}

		@Override
		public RatioValue makeResetNext() {
			return new RatioValue(numerator, denominator, count, min, max, true);
		}

		@Override
		public RatioValue makeAdjusted(NumeratorDenominator value) {
			if (resetNext) {
				double result = value.numerator / value.denominator;
				return new RatioValue(value.numerator, value.denominator, 1, result, result, false);
			}

			double newNumerator;
			double newDenominator;
			double min;
			double max;
			if (count == 0) {
				newNumerator = value.numerator;
				newDenominator = value.denominator;
				min = newNumerator / newDenominator;
				max = min;
			} else {
				/*
				 * This is a bit tricky. When adjusting the value, we must multiple the values together and not add
				 * them. If we add them then the last adjustment would get the most weight if it is a straight average.
				 * 
				 * So we basically do the cross product addition mechanism each time we adjust by another value and we
				 * will divide by the count at the very end when the value is retrieved.
				 * 
				 * So if we are adding 1/2 + 1/3, we should get 5/6. 5/6 + 1/4 = 26/24. 26/24 + 1/2 = 76/48. Then the
				 * final result is 76/(48*4) = 0.395833333.
				 */
				newNumerator = this.numerator * value.denominator + value.numerator * this.denominator;
				// denominator is the current denominator times the incoming one so 1/2 + 1/3 is some number of 6ths
				newDenominator = this.denominator * value.denominator;

				double result = newNumerator / newDenominator;
				if (result < this.min) {
					min = result;
					max = this.max;
				} else if (result > this.max) {
					min = this.min;
					max = result;
				} else {
					min = this.min;
					max = this.max;
				}
			}
			return new RatioValue(newNumerator, newDenominator, count + 1, min, max, false);
		}

		@Override
		public boolean isResetNext() {
			return resetNext;
		}

		@Override
		public Number getValue() {
			double value;
			double adjustedDenominator = denominator * count;
			if (adjustedDenominator == 0) {
				// protect against div by 0
				value = 0;
			} else {
				value = numerator / adjustedDenominator;
			}
			return Double.valueOf(value);
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
