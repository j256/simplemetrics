package com.j256.simplemetrics.metric;

/**
 * Metric which defines some common fields and methods.
 * 
 * @param <V>
 *            Value type that we use to adjust this metric-value.
 * @param <MV>
 *            Metric value which wraps and manages the raw value.
 * 
 * @author graywatson
 */
public interface ControlledMetric<V, MV extends MetricValue<V, MV>> {

	/**
	 * Component name of the metric. This can be something like "heap".
	 */
	public String getComponent();

	/**
	 * Module name of the metric or null if none. This can be something like "oldGen".
	 */
	public String getModule();

	/**
	 * Component name of the metric. This can be something like "usedPercentage".
	 */
	public String getName();

	/**
	 * Description of the component which is not persisted. Here for code documentation purposes and JMX.
	 */
	public String getDescription();

	/**
	 * Unit of the metric such as percentage, megabytes, or per-second.
	 */
	public String getUnit();

	/**
	 * Return the aggregation type of the metric.
	 */
	public String getAggregationTypeName();

	/**
	 * Create the initial value for our metric.
	 */
	public MV createInitialValue();

	/**
	 * Adjust the value by a long primitive value. If this is an accumulator then it will be added. If this is a value
	 * then it will bet set.
	 */
	public void adjustValue(long value);

	/**
	 * Adjust the metric with this value. If this is an accumulator then it will be added. If this is a value then it
	 * will bet set.
	 */
	public void adjustValue(Number value);

	/**
	 * Make a value type from long input. This is only needed to support the {@link #adjustValue(long)} method.
	 */
	public V makeValueFromLong(long value);

	/**
	 * Make a value type from Number input. This is only needed to support the {@link #adjustValue(Number)} method.
	 */
	public V makeValueFromNumber(Number number);

	/**
	 * Returned the value of the metric as a number.
	 */
	public Number getValue();

	/**
	 * Return the value details of the metric.
	 */
	public MetricValueDetails getValueDetails();

	/**
	 * Return the value of the metric as a number suitable to be persisted.
	 */
	public Number getValueToPersist();

	/**
	 * Return the value details of the metric suitable to be persisted.
	 */
	public MetricValueDetails getValueDetailsToPersist();

	/**
	 * Returns the type of aggregation used by this metric.
	 */
	public AggregationType getAggregationType();

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
