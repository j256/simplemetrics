package com.j256.simplemetrics.metric;

/**
 * Metric values that wraps a raw metric value.
 * 
 * @param <V>
 *            Value type that we use to adjust this metric-value.
 * @param <MV>
 *            MetricValue type that holds the metric-value information. We need this because of
 *            {@link #makeAdjusted(Object)} which needs to be the same type.
 */
public interface MetricValue<V, MV extends MetricValue<V, MV>> {

	/**
	 * Make an entry that is suitable for persisting. We do this so the metric retains its value and doesn't immediately
	 * drop to 0 or something immediately after a persist event. The next time we adjust the value or we persist it
	 * again, the value will be reset beforehand.
	 */
	public MV makePersisted();

	/**
	 * Make a new entry adjusted by the value parameter.
	 */
	public MV makeAdjusted(V value);

	/**
	 * Get the number value from this metric value. This is a transient value and {@link #makePersisted()} should be
	 * used if you want to save the value to disk. The transient one is good for JMX or other direct monitoring of the
	 * metric.
	 */
	public Number getValue();

	/**
	 * Get the number of samples from the metric value.
	 */
	public int getNumSamples();

	/**
	 * Get the minimum value of the metric since the last persist. Will be 0 if num-samples is 0.
	 */
	public Number getMin();

	/**
	 * Get the maximum value of the metric since the last persist. Will be 0 if num-samples is 0.
	 */
	public Number getMax();
}
