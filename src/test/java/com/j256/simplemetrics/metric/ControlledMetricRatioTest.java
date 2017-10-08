package com.j256.simplemetrics.metric;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.j256.simplemetrics.metric.ControlledMetric.AggregationType;

public class ControlledMetricRatioTest {

	@Test
	public void testStuff() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		assertEquals(AggregationType.AVERAGE, metric.getAggregationType());
		long num1 = 1;
		long denom1 = 2;
		metric.adjustValue(num1, denom1);
		assertEquals((double) num1 / (double) denom1, (Double) metric.getValue(), 0);
		assertEquals((double) num1 / (double) denom1, (Double) metric.getValueDetails().getMin(), 0);
		assertEquals((double) num1 / (double) denom1, (Double) metric.getValueDetails().getMax(), 0);
		assertEquals(1, metric.getValueDetails().getNumSamples());
		assertEquals(AggregationType.AVERAGE, metric.getAggregationType());

		long num2 = 1;
		long denom2 = 3;
		metric.adjustValue(num2, denom2);

		double result2 = (((double) num1 / (double) denom1) + ((double) num2 / (double) denom2)) / 2.0;
		assertEquals(result2, (Double) metric.getValue(), 0.00001);
		assertEquals(result2, (Double) metric.getValueDetails().getMin(), 0.0001);
		assertEquals((double) num1 / (double) denom1, (Double) metric.getValueDetails().getMax(), 0);
		assertEquals(2, metric.getValueDetails().getNumSamples());

		long num3 = 9;
		long denom3 = 1;
		metric.adjustValue(num3, denom3);

		double result3 = (((double) num1 / (double) denom1) + ((double) num2 / (double) denom2)
				+ ((double) num3 / (double) denom3)) / 3.0;
		assertEquals(result3, (Double) metric.getValue(), 0.00001);
		assertEquals(result2, (Double) metric.getValueDetails().getMin(), 0.0001);
		assertEquals(result3, (Double) metric.getValueDetails().getMax(), 0.0001);
		assertEquals(3, metric.getValueDetails().getNumSamples());
	}

	@Test
	public void testNoAdjustments() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		assertEquals(0.0, metric.getValue());
	}

	@Test
	public void testDoublePersist() {
		ControlledMetricRatio metric = new ControlledMetricRatio("component", "module", "name", "desc", null);
		long num = 1;
		long denom = 2;
		metric.adjustValue(num, denom);
		assertEquals((double) num / (double) denom, metric.getValue());
		assertEquals((double) num / (double) denom, metric.getValueToPersist());
		// even after the double-persist, the value shouldn't be changed
		assertEquals((double) num / (double) denom, metric.getValue());
		assertEquals((double) num / (double) denom, metric.getValueToPersist());
		assertEquals((double) num / (double) denom, metric.getValue());

		num = 1;
		denom = 3;
		// but after we adjust again, it should be reset and the previous value shouldn't have been saved
		metric.adjustValue(num, denom);
		assertEquals((double) num / (double) denom, metric.getValue());
		assertEquals((double) num / (double) denom, metric.getValueToPersist());
		assertEquals((double) num / (double) denom, metric.getValue());
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
