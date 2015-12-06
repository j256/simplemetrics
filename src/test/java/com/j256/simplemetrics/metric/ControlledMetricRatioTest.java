package com.j256.simplemetrics.metric;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ControlledMetricRatioTest {

	@Test
	public void testStuff() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		long num = 1;
		long denom = 2;
		metric.adjustValue(num, denom);
		assertEquals((double) num / (double) denom, (Double) metric.getValue(), 0);
	}

	@Test
	public void testNoAdjustments() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		assertEquals(0.0, metric.getValue());
	}

	@Test
	public void testDoublePersist() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);;
		long num = 1;
		long denom = 2;
		metric.adjustValue(num, denom);
		Number details = metric.getValueToPersist();
		assertEquals((double) num / (double) denom, (Double) details, 0.5);
		assertEquals((double) num / (double) denom, (Double) metric.getValue(), 0.5);
		details = metric.getValueToPersist();
		assertEquals(0.0, details);
	}

	@Test
	public void testAdjustOldLong() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		long num1 = 2;
		metric.adjustValue(num1);
		long num2 = 2;
		metric.adjustValue(num2);
		assertEquals((double) (num1 + num2) / (double) 2, (Double) metric.getValue(), 0);
	}

	@Test
	public void testAdjustOldNumberSmall() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		double num1 = 0.2;
		metric.adjustValue(num1);
		double num2 = 0.4;
		metric.adjustValue(num2);
		assertEquals((double) (num1 + num2) / (double) 2, (Double) metric.getValue(), 0);
	}

	@Test
	public void testAdjustOldNumberLarge() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		double num1 = 2011323;
		metric.adjustValue(num1);
		double num2 = 4023131;
		metric.adjustValue(num2);
		assertEquals((double) (num1 + num2) / (double) 2, (Double) metric.getValue(), 0);
	}

	@Test
	public void testBasic() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		long numTotal = 0;
		long denomTotal = 0;
		long numAdd = 1;
		long numDenom = 2;
		metric.adjustValue(numAdd, numDenom);
		numTotal += numAdd;
		denomTotal += numDenom;
		metric.adjustValue(numAdd, numDenom);
		numTotal += numAdd;
		denomTotal += numDenom;
		Number details = metric.getValue();
		assertEquals((double) numTotal / (double) denomTotal, (Double) details, 0);
	}

	@Test
	public void testReset() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		long num1 = 1;
		long denom1 = 2;
		metric.adjustValue(num1, denom1);
		Number details = metric.getValue();
		assertEquals((double) num1 / (double) denom1, (Double) details, 0);

		long num2 = 1;
		long denom2 = 3;
		metric.adjustValue(num2, denom2);
		details = metric.getValue();
		assertEquals(0.4166666666, (Double) details, 0.0000000001);

		long num3 = 1;
		long denom3 = 4;
		metric.adjustValue(num3, denom3);
		details = metric.getValue();
		assertEquals(0.3611111111, (Double) details, 0.0000000001);

		long num4 = 1;
		long denom4 = 2;
		metric.adjustValue(num4, denom4);
		details = metric.getValue();
		assertEquals(0.3958333333, (Double) details, 0.0000000001);
	}
}
