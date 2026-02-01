package com.j256.simplemetrics.persister;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetric.AggregationType;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

public class CloudWatchMetricsPersisterTest {

	@Test
	public void testStuff() throws IOException {
		MetricsManager manager = new MetricsManager();
		CloudWatchMetricsPersister persister = new CloudWatchMetricsPersister();
		String appName = getClass().getSimpleName();
		persister.setApplicationName(appName);
		CloudWatchClient cloudWatchClient = createMock(CloudWatchClient.class);
		persister.setCloudWatchClient(cloudWatchClient);
		persister.setAddInstanceData(true);
		String nameSpacePrefix = "ns";
		persister.setNameSpacePrefix(nameSpacePrefix);
		persister.initialize();
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { persister });
		String comp = "test";
		StandardUnit unit = StandardUnit.COUNT;
		int value = 3;
		int NUM_METRICS = CloudWatchMetricsPersister.MAX_NUM_DATUM_ALLOWED_PER_POST;
		for (int i = 0; i < NUM_METRICS; i++) {
			ControlledMetricAccum metric = new ControlledMetricAccum(comp, "stuff", "loop" + i, null, unit.name());
			manager.registerMetric(metric);
			metric.add(value);
		}

		ControlledMetricValue valueMetric = new ControlledMetricValue(comp, null, "value1", null, unit.name());
		manager.registerMetric(valueMetric);
		valueMetric.adjustValue(value);
		ControlledMetricAccum accumMetric = new ControlledMetricAccum(comp, null, "accum1", null, "unknown");
		manager.registerMetric(accumMetric);
		// no add to this metric

		ControlledMetricAccum accumMetric2 = new ControlledMetricAccum(comp, null, "accum2", null, null);
		manager.registerMetric(accumMetric2);
		accumMetric2.add(Integer.MAX_VALUE + 1L);

		List<MetricDatum> data = new ArrayList<MetricDatum>();
		for (ControlledMetric<?, ?> metric : manager.getMetricValueDetailsMap().keySet()) {
			MetricDatum.Builder datumBuilder = MetricDatum.builder();
			datumBuilder.metricName(metric.getName());
			if (metric.getModule() == null) {
				datumBuilder.dimensions(Dimension.builder().name("Component").value(metric.getComponent()).build());
			} else {
				datumBuilder.dimensions(Dimension.builder().name("Component").value(metric.getComponent()).build(),
						Dimension.builder().name("Module").value(metric.getModule()).build());
			}
			if (metric.getAggregationType() == AggregationType.SUM) {
				double sampleCount = metric.getValue().doubleValue();
				double sum = sampleCount;
				if (sum > Integer.MAX_VALUE) {
					sum = Integer.MAX_VALUE;
					sampleCount = Integer.MAX_VALUE;
				}
				if (metric.getValue().longValue() == 0) {
					sampleCount = CloudWatchMetricsPersister.ZERO_NUM_SAMPLES_REPLACEMENT;
				}
				datumBuilder.statisticValues(
						StatisticSet.builder().sampleCount(sampleCount).minimum(1.0).maximum(1.0).sum(sum).build());
			} else {
				datumBuilder.value(Double.valueOf(value));
			}
			if (metric.getUnit() == null) {
				datumBuilder.unit(StandardUnit.NONE);
			} else {
				datumBuilder.unit(StandardUnit.COUNT);
			}
			data.add(datumBuilder.build());
			if (data.size() >= CloudWatchMetricsPersister.MAX_NUM_DATUM_ALLOWED_PER_POST) {
				PutMetricDataRequest.Builder builder = PutMetricDataRequest.builder();
				builder.namespace(nameSpacePrefix + ": " + appName);
				builder.metricData(data);
				expect(cloudWatchClient.putMetricData(builder.build())).andReturn(null);
				data = new ArrayList<MetricDatum>();
			}
		}
		if (data.size() > 0) {
			PutMetricDataRequest.Builder builder = PutMetricDataRequest.builder();
			builder.namespace(nameSpacePrefix + ": " + appName);
			builder.metricData(data);
			expect(cloudWatchClient.putMetricData(builder.build())).andReturn(null);
		}

		replay(cloudWatchClient);
		manager.persist();
		verify(cloudWatchClient);
	}
}
