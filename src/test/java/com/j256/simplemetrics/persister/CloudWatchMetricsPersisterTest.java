package com.j256.simplemetrics.persister;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.metric.ControlledMetricAccum;

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
		manager.setMetricDetailsPersisters(new MetricDetailsPersister[] { persister });
		String comp = "test";
		String mod = "stuff";
		String name = "counter";
		StandardUnit unit = StandardUnit.Count;
		ControlledMetricAccum metric = new ControlledMetricAccum(comp, mod, name, null, unit.name());
		manager.registerMetric(metric);

		int value = 3;
		PutMetricDataRequest request = new PutMetricDataRequest();
		request.setNamespace("Application: " + appName);
		MetricDatum datum = new MetricDatum();
		datum.setMetricName(name);
		datum.withDimensions(new Dimension().withName("Component").withValue(comp), new Dimension().withName("Module")
				.withValue(mod));
		datum.withStatisticValues(new StatisticSet().withSampleCount(Double.valueOf(value))
				.withMinimum(1.0)
				.withMaximum(1.0)
				.withSum(Double.valueOf(value)));
		datum.withUnit(StandardUnit.Count);

		List<MetricDatum> data = new ArrayList<MetricDatum>();
		data.add(datum);
		request.setMetricData(data);
		cloudWatchClient.putMetricData(request);

		metric.add(value);
		replay(cloudWatchClient);
		manager.persist();
		verify(cloudWatchClient);
	}
}
