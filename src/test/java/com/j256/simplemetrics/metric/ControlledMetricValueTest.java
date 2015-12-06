package com.j256.simplemetrics.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.junit.Test;

public class ControlledMetricValueTest {

	@Test
	public void testControlledMetricStringStringString() {
		String name = "label";
		String component = "comp";
		String module = "mod";
		String description = "desc";
		String unit = "unit";
		ControlledMetricValue metric = new ControlledMetricValue(component, module, name, description, unit);
		assertEquals(component, metric.getComponent());
		assertEquals(module, metric.getModule());
		assertEquals(name, metric.getName());
		assertEquals(unit, metric.getUnit());
		assertEquals(name, metric.getJmxBeanName());
		assertEquals(2, metric.getJmxFolderNames().length);
		assertEquals(component, metric.getJmxFolderNames()[1].getValue());
		assertEquals(description, metric.getDescription());
	}

	@Test
	public void testDoublePersist() {
		ControlledMetricValue metric = new ControlledMetricValue("c", "m", "n", "d", "u");;
		long val = 10;
		metric.adjustValue(val);
		Number details = metric.getValueToPersist();
		assertEquals(Double.valueOf(val), details);
		assertEquals(Double.valueOf(val), metric.getValue());
		details = metric.getValueToPersist();
		assertEquals(0.0, details);
		assertEquals(0.0, metric.getValue());
	}

	@Test
	public void testControlledMetricStringStringStringLong() {
		ControlledMetricValue metric = new ControlledMetricValue("c", "m", "n", "d", "u");;
		assertEquals(0.0, metric.getValue());
		long val = 10021;
		metric.adjustValue(val);
		assertEquals(Double.valueOf(val), metric.getValue());
	}

	@Test
	public void testUpdateMetric() {
		ControlledMetricValue metric = new ControlledMetricValue("c", "m", "n", "d", "u");
		long first = 100;
		metric.adjustValue(first);
		assertEquals(Double.valueOf(first), metric.getValue());
		long second = 1;
		metric.adjustValue(second);
		assertEquals((first + second) / 2.0, metric.getValue());
	}

	@Test(expected = NullPointerException.class)
	public void testControlledLabelNull() {
		new ControlledMetricValue("comp", "mod", null, "desc", "unit");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testControlledLabelEmpty() {
		new ControlledMetricValue("comp", "mod", "", "desc", "unit");
	}

	@Test(expected = NullPointerException.class)
	public void testControlledCompNull() {
		new ControlledMetricValue(null, "mod", "name", "desc", "unit");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testControlledCompEmpty() {
		new ControlledMetricValue("", "mod", "name", "desc", "unit");
	}

	@Test
	public void testControlledDescNull() {
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "name", null, "unit");
		assertNull(metric.getDescription());
	}

	@Test
	public void testControlledDescEmpty() {
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "name", "", "unit");
		assertNull(metric.getDescription());
	}

	@Test
	public void testSetOverTime() throws Exception {

		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", "name", "desc", "unit");
		long duration = 500;
		int numIncrements = 10;
		long bucketDelay = duration / numIncrements;

		int total = 0;
		Random random = new Random();
		for (int x = 0; x < numIncrements; x++) {
			int value = random.nextInt(1000000000) / numIncrements;
			metric.adjustValue(value);
			total += value;
			// System.out.println("Added value " + value);
			Thread.sleep(bucketDelay + 1);
		}
		long value = metric.getValue().longValue();
		long expected = total / numIncrements;
		assertEquals(expected, value);
	}

	@Test
	public void testPrecision() {
		ControlledMetricValue metric = new ControlledMetricValue("c", "m", "n", "d", "u");
		double value = 101.123;
		metric.adjustValue(value);
		Number details = metric.getValue();
		assertEquals(value, (Double) details, 0.00);
		long newValue = 200;
		metric.adjustValue(newValue);
		details = metric.getValue();
		assertEquals(((double) 200 + 101.123) / 2, (Double) details, 0);
	}
}
