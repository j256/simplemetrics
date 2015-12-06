package com.j256.simplemetrics.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.j256.simplemetrics.metric.ControlledMetricAccum;

public class ControlledMetricAccumTest {

	@Test
	public void testControlledMetricStringStringString() {
		String name = "n";
		String component = "c";
		String module = "m";
		String description = "d";
		String unit = "u";
		ControlledMetricAccum metric = new ControlledMetricAccum(component, module, name, description, unit);
		assertEquals(name, metric.getName());
		assertEquals(name, metric.getJmxBeanName());
		assertEquals(2, metric.getJmxFolderNames().length);
		assertEquals(component, metric.getJmxFolderNames()[1].getValue());
		assertEquals(description, metric.getDescription());
		assertEquals(unit, metric.getUnit());
	}

	@Test
	public void testControlledMetricStringStringStringLong() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);;
		assertEquals(0L, metric.getValue());
		long value = 10021;
		metric.add(value);
		assertEquals(value, metric.getValue());
	}

	@Test
	public void testDoublePersist() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);;
		long num = 100;
		metric.adjustValue(num);
		Number details = metric.getValueToPersist();
		assertEquals(num, details);
		assertEquals(num, metric.getValue());
		details = metric.getValueToPersist();
		assertEquals(0L, details);
		assertEquals(0L, metric.getValue());
	}

	@Test
	public void testAddToMetric() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);
		long delta = 100;
		metric.add(delta);
		assertEquals(delta, metric.getValue());
		metric.increment();
		assertEquals(delta + 1, metric.getValue());
	}

	@Test(expected = NullPointerException.class)
	public void testControlledLabelNull() {
		new ControlledMetricAccum("c", "m", null, "d", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testControlledLabelEmpty() {
		new ControlledMetricAccum("c", "m", "", "d", null);
	}

	@Test(expected = NullPointerException.class)
	public void testControlledCompNull() {
		new ControlledMetricAccum(null, "m", "n", "d", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testControlledCompEmpty() {
		new ControlledMetricAccum("", "m", "n", "d", null);
	}

	@Test
	public void testControlledDescNull() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", null, null);
		assertNull(metric.getDescription());
	}

	@Test
	public void testControlledDescEmpty() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "", null);
		assertNull(metric.getDescription());
	}

	@Test
	public void testSetOverTime() throws Exception {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);
		long duration = 500;

		int delta = 4;
		int numberIncrements = 5;
		metric.add(0);
		for (int x = 0; x < numberIncrements; x++) {
			metric.add(delta);
			Thread.sleep(1 + (duration / numberIncrements));
		}

		assertEquals((long) (delta * numberIncrements), metric.getValue());
	}
}
