package com.j256.simplemetrics.metric;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.j256.simplejmx.common.JmxFolderName;

public class ControlledMetricJmxTest {

	@Test
	public void testControlledMetricStringStringString() {
		String name = "n";
		String component = "c";
		String module = "m";
		String description = "d";
		String unit = "u";
		ControlledMetricAccum metric = new ControlledMetricAccum(component, module, name, description, unit);
		ControlledMetricJmx metricJmx =
				new ControlledMetricJmx(metric, "com.j256", new JmxFolderName[] { new JmxFolderName("metrics") });
		assertEquals(name, metricJmx.getName());
		assertEquals(name, metricJmx.getJmxBeanName());
		assertEquals(2, metricJmx.getJmxFolderNames().length);
		assertEquals(component, metricJmx.getJmxFolderNames()[1].getValue());
		assertEquals(description, metricJmx.getDescription());
		assertEquals(unit, metricJmx.getUnit());
	}
}
