package com.j256.simplemetrics.metric;


/**
 * Value information for the metric shared externally.
 * 
 * @author graywatson
 */
public class MetricValueDetails {

	private final Number number;
	private final int numSamples;
	private final Number min;
	private final Number max;

	public MetricValueDetails(MetricValue<?, ?> metricValue) {
		this.number = metricValue.getNumber();
		this.numSamples = metricValue.getNumSamples();
		this.min = metricValue.getMin();
		this.max = metricValue.getMax();
	}

	/**
	 * Get the number from this metric value.
	 */
	public Number getNumber() {
		return number;
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
}
