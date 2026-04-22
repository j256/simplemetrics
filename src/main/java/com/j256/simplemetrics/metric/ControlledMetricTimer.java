package com.j256.simplemetrics.metric;

/**
 * Convenience to allow tracking of the elapsed time of events as a metric. Basically you do a:
 * 
 * <pre>
 * ControlledMetricTimer timer = new ControlledMetricTimer(...);
 * ...
 * long millis = timer.start();
 * dao.createEntry(...);
 * timer.stopAndAdd(millis);
 * </pre>
 * 
 * The rest is done by the class and the metric system.
 * 
 * @author graywatson
 */
public class ControlledMetricTimer extends ControlledMetricValue {

	/**
	 * @param component
	 *            Component short name such as "my".
	 * @param module
	 *            Module name to identify the part of the component such as "pageview".
	 * @param name
	 *            String label description the metric.
	 * @param description
	 *            Description for more information which may not be persisted.
	 */
	public ControlledMetricTimer(String component, String module, String name, String description) {
		super(component, module, name, description, "milliseconds");
	}

	/**
	 * Start the timer on a particular event. You should call the {@link #stopAndAdd(long)} method after the event that
	 * you want to track completes.
	 * 
	 * @return Millis which should be passed to {@link #stop(long)} as the argument.
	 */
	public long start() {
		return System.currentTimeMillis();
	}

	/**
	 * @deprecated Please use {@link #stopAndAdd(long)}.
	 */
	@Deprecated
	public long stop(long startMillis) {
		return stopAndAdd(startMillis);
	}

	/**
	 * Stop the timer of a particular event. This will calculate the time difference and add this to the counter.
	 * 
	 * @param startMillis
	 *            Value returned from a previous call to {@link #start()}.
	 * 
	 * @return the calculated time difference added to the counter
	 */
	public long stopAndAdd(long startMillis) {
		long elapsed = System.currentTimeMillis() - startMillis;
		adjustValue(elapsed);
		return elapsed;
	}
}
