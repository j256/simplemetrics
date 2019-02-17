package com.j256.simplemetrics.metric;

/**
 * Convenience to allow tracking of the elapsed time of events as a metric. Basically you do a:
 * 
 * <pre>
 * ControlledMetricTimer timer = new ControlledMetricTimer(...);
 * ...
 * long millis = timer.start();
 * dao.createEntry(...);
 * timer.end(millis);
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
	 * Start the timer on a particular event. You should wrap the start and the end around the event you want to track.
	 * 
	 * @return Millis which should be passed to {@link #stop(long)} as the argument.
	 */
	public long start() {
		return System.currentTimeMillis();
	}

	/**
	 * End the timer on a particular event. This will calculate the time difference and add this to the counter.
	 * 
	 * @param startMillis
	 *            Value returned from a previous call to {@link #start()}.
	 * 
	 * @return the calculated time difference added to the counter
	 */
	public long stop(long startMillis) {
		long elapsed = System.currentTimeMillis() - startMillis;
		adjustValue(elapsed);
		return elapsed;
	}
}
