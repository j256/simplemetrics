package com.j256.simplemetrics.metric;

/**
 * Value detail information for the metric.
 * 
 * @author graywatson
 */
public class MetricValueDetails {

	private final Number value;
	private final int numSamples;
	private final Number min;
	private final Number max;

	public MetricValueDetails(MetricValue<?, ?> metricValue) {
		Number value = metricValue.getValue();
		// convert the value to a long if possible
		if (value.doubleValue() == value.longValue()) {
			this.value = value.longValue();
		} else {
			this.value = value;
		}
		this.numSamples = metricValue.getNumSamples();
		this.min = metricValue.getMin();
		this.max = metricValue.getMax();
	}

	/**
	 * Get the number from this metric value.
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * Get the number of samples from the metric value.
	 */
	public int getNumSamples() {
		return numSamples;
	}

	/**
	 * Get the minimum value of the metric since the last persist. Will be 0 if num-samples is 0.
	 */
	public Number getMin() {
		return min;
	}

	/**
	 * Get the maximum value of the metric since the last persist. Will be 0 if num-samples is 0.
	 */
	public Number getMax() {
		return max;
	}

	@Override
	public String toString() {
		return "MetricValueDetails [value=" + value + ", numSamples=" + numSamples + ", min=" + min + ", max=" + max
				+ "]";
	}
}
