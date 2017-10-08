package com.j256.simplemetrics.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

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
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);
		assertEquals(0L, metric.getValue());
		long value = 10021;
		metric.add(value);
		assertEquals(value, metric.getValue());
	}

	@Test
	public void testDoublePersist() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);
		long num = 100;
		metric.adjustValue(num);
		assertEquals(num, metric.getValue());
		assertEquals(num, metric.getValueToPersist());
		assertEquals(num, metric.getValue());
		assertEquals(num, metric.getValueToPersist());
		assertEquals(num, metric.getValue());

		// now that we have persisted, if we get another adjustment, it should reset with no sign of previous
		num = 50;
		metric.adjustValue(num);
		assertEquals(num, metric.getValue());
		assertEquals(num, metric.getValueToPersist());
		assertEquals(num, metric.getValue());
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

	@Test
	public void testMetricDetails() {
		ControlledMetricAccum metric = new ControlledMetricAccum("c", "m", "n", "d", null);
		long delta = 100;
		metric.add(delta);
		MetricValueDetails details = metric.getValueDetails();
		assertEquals(delta, details.getNumSamples());
		assertEquals(delta, details.getMin());
		assertEquals(delta, details.getMax());
		assertEquals(delta, details.getValue());
		// coverage
		details.toString();
	}

	@Test
	public void testCoverage() {
		String comp = "c";
		String mod = "m";
		String name = "n";
		ControlledMetricAccum metric = new ControlledMetricAccum(comp, mod, name, "d", null);
		assertEquals(comp, metric.getComponent());
		assertEquals(mod, metric.getModule());
		assertEquals(name, metric.getName());
		assertEquals("SUM", metric.getAggregationTypeName());
		assertEquals(0, metric.compareTo(metric));
		ControlledMetricAccum metric2 = new ControlledMetricAccum("d", mod, name, "d", null);
		assertEquals(-1, metric.compareTo(metric2));
		ControlledMetricAccum metric3 = new ControlledMetricAccum(comp, "n", name, "d", null);
		assertEquals(-1, metric.compareTo(metric3));
		ControlledMetricAccum metric4 = new ControlledMetricAccum(comp, null, name, "d", null);
		assertEquals(1, metric.compareTo(metric4));
		assertEquals(0, metric4.compareTo(metric));
		metric.makeValueFromNumber((Number) 1);

		Set<ControlledMetricAccum> metrics = new HashSet<ControlledMetricAccum>();
		metrics.add(metric);
		assertTrue(metrics.contains(new ControlledMetricAccum(comp, mod, name, "d", null)));
		metrics.add(metric4);
		assertTrue(metrics.contains(metric4));

		assertFalse(metric.equals(null));
		assertFalse(metric.equals(this));
		assertFalse(metric.equals(metric2));
		assertFalse(metric.equals(metric3));
		assertFalse(metric.equals(metric4));
		assertFalse(metric4.equals(metric));
		assertTrue(metric4.equals(metric4));
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
