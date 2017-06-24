package com.j256.simplemetrics.persister;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetric.AggregationType;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;

public class CloudWatchMetricsPersisterTest {

	@Test
	public void testStuff() throws IOException {
		MetricsManager manager = new MetricsManager();
		CloudWatchMetricsPersister persister = new CloudWatchMetricsPersister();
		String appName = getClass().getSimpleName();
		persister.setApplicationName(appName);
		AmazonCloudWatch cloudWatchClient = createMock(AmazonCloudWatch.class);
		persister.setCloudWatchClient(cloudWatchClient);
		persister.setAddInstanceData(true);
		String nameSpacePrefix = "ns";
		persister.setNameSpacePrefix(nameSpacePrefix);
		persister.initialize();
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { persister });
		String comp = "test";
		StandardUnit unit = StandardUnit.Count;
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
			MetricDatum datum = new MetricDatum();
			datum.setMetricName(metric.getName());
			if (metric.getModule() == null) {
				datum.withDimensions(new Dimension().withName("Component").withValue(metric.getComponent()));
			} else {
				datum.withDimensions(new Dimension().withName("Component").withValue(metric.getComponent()),
						new Dimension().withName("Module").withValue(metric.getModule()));
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
				datum.withStatisticValues(new StatisticSet().withSampleCount(sampleCount)
						.withMinimum(1.0)
						.withMaximum(1.0)
						.withSum(sum));
			} else {
				datum.setValue(Double.valueOf(value));
			}
			if (metric.getUnit() == null) {
				datum.withUnit(StandardUnit.None);
			} else {
				datum.withUnit(StandardUnit.Count);
			}
			data.add(datum);
			if (data.size() >= CloudWatchMetricsPersister.MAX_NUM_DATUM_ALLOWED_PER_POST) {
				PutMetricDataRequest request = new PutMetricDataRequest();
				request.setNamespace(nameSpacePrefix + ": " + appName);
				request.setMetricData(data);
				cloudWatchClient.putMetricData(request);
				data = new ArrayList<MetricDatum>();
			}
		}
		if (data.size() > 0) {
			PutMetricDataRequest request = new PutMetricDataRequest();
			request.setNamespace(nameSpacePrefix + ": " + appName);
			request.setMetricData(data);
			cloudWatchClient.putMetricData(request);
		}

		replay(cloudWatchClient);
		manager.persist();
		verify(cloudWatchClient);
	}

	@Test
	public void testCoverage() {
		AWSCredentials creds = new BasicAWSCredentials("key", "secret");
		CloudWatchMetricsPersister persister = new CloudWatchMetricsPersister(creds, "app", true);
		StaticCredentialsProvider credProv = new StaticCredentialsProvider(creds);
		persister = new CloudWatchMetricsPersister(new AWSCredentialsProviderChain(credProv), "app", true);
		persister = new CloudWatchMetricsPersister();
		persister.setAwsCredentials(creds);
		persister.setAwsCredentialsProvider(credProv);
	}

}
