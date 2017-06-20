package com.j256.simplemetrics.persister;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.StatisticSet;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetric.AggregationType;
import com.j256.simplemetrics.metric.MetricValueDetails;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Class which persists our metrics to Amazon's AWS CloudWatch cloud service. This requires the aws-java-sdk package to
 * be in the classpath to compile.
 * 
 * <p>
 * <b>NOTE:</b> If you are using the no-arg constructor (like with Spring) you will need to make sure that
 * {@link #initialize()} is called.
 * </p>
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" }, beanName = "CloudWatchPersister",
		description = "CloudWatch Metrics Persister")
public class CloudWatchMetricsPersister implements MetricDetailsPersister {

	private static final String DEFAULT_NAME_SPACE_PREFIX = "Application";
	private static final String INSTANCE_ID_DIMENSION = "InstanceId";
	private static final String COMPONENT_DIMENSION = "Component";
	private static final String MODULE_DIMENSION = "Module";
	private static final int MAX_NUM_DATUM_ALLOWED_PER_POST = 20;
	private static final StandardUnit DEFAULT_AWS_UNIT = StandardUnit.Count;
	private static final double ZERO_NUM_SAMPLES_REPLACEMENT = 0.000000001D;

	private static final String AWS_INSTANCE_INFO_IP = "169.254.169.254";
	private static final String INSTANCE_ID_FETCH_PATH = "/latest/meta-data/instance-id";
	private static final int AWS_CONNECT_TIMEOUT_MILLIS = 2000;

	// makes sure that we are running in EC2 and extracts the instance-id name
	private static final Pattern INSTANCE_ID_RESULT_REGEX = Pattern.compile("(?s).*Server: EC2ws.*\r\n\r\n(.*)");
	private static final Map<String, StandardUnit> AWS_UNIT_MAP = new HashMap<String, StandardUnit>();

	private String applicationName = "unknown";
	private String nameSpacePrefix = DEFAULT_NAME_SPACE_PREFIX;
	private AWSCredentials awsCredentials;
	private boolean addInstanceData = true;

	private AmazonCloudWatch cloudWatchClient;
	private static String instanceId;

	static {
		for (StandardUnit unit : StandardUnit.values()) {
			String name = unit.name().toLowerCase();
			AWS_UNIT_MAP.put(name, unit);
			if (name.endsWith("s")) {
				// strip off the plural S at the end so we match megabyte as well
				AWS_UNIT_MAP.put(name.substring(0, name.length() - 1), unit);
			}
		}

		// force some mappings
		AWS_UNIT_MAP.put("kb", StandardUnit.Kilobytes);
		AWS_UNIT_MAP.put("mb", StandardUnit.Megabytes);
		AWS_UNIT_MAP.put("gb", StandardUnit.Gigabytes);
		AWS_UNIT_MAP.put("mbit", StandardUnit.Megabits);
		AWS_UNIT_MAP.put("millis", StandardUnit.Milliseconds);
		AWS_UNIT_MAP.put("ms", StandardUnit.Milliseconds);
		AWS_UNIT_MAP.put("msec", StandardUnit.Milliseconds);
		AWS_UNIT_MAP.put("msecs", StandardUnit.Milliseconds);
		AWS_UNIT_MAP.put("bps", StandardUnit.BytesSecond);
		AWS_UNIT_MAP.put("%", StandardUnit.Percent);
		AWS_UNIT_MAP.put("percentage", StandardUnit.Percent);
		AWS_UNIT_MAP.put("many", StandardUnit.Count);
	}

	public CloudWatchMetricsPersister() {
		// for spring
	}

	public CloudWatchMetricsPersister(AWSCredentials awsCredentials, String applicationName, boolean addInstanceData) {
		this.awsCredentials = awsCredentials;
		this.applicationName = applicationName;
		this.addInstanceData = addInstanceData;
		initialize();
	}

	/**
	 * Should be called if the no-arg construct is being used and after the file metrics have been set. Maybe by Springs
	 * init mechanism?
	 */
	public void initialize() {
		if (cloudWatchClient == null) {
			cloudWatchClient = new AmazonCloudWatchClient(awsCredentials);
		}
		if (addInstanceData) {
			instanceId = downloadInstanceId(AWS_CONNECT_TIMEOUT_MILLIS);
			// NOTE: instanceId could be null
		}
	}

	@Override
	public synchronized void persist(Map<ControlledMetric<?, ?>, MetricValueDetails> metricValues, long timeMillis)
			throws IOException {

		Map<String, List<MetricDatum>> metricMap = buildMetricsMap(metricValues);

		// now write them to cloud-watch
		for (Map.Entry<String, List<MetricDatum>> entry : metricMap.entrySet()) {

			String nameSpace = nameSpacePrefix + ": " + MiscUtils.capitalize(entry.getKey());

			List<MetricDatum> datumList = entry.getValue();

			// we need to build multiple requests to post X datum at a time
			int endIndex;
			for (int startIndex = 0; startIndex < datumList.size(); startIndex = endIndex) {
				endIndex = startIndex + MAX_NUM_DATUM_ALLOWED_PER_POST;
				if (endIndex > datumList.size()) {
					endIndex = datumList.size();
				}

				List<MetricDatum> requestDatumList;
				if (startIndex == 0 && endIndex == datumList.size()) {
					// no need to make a sub-list
					requestDatumList = datumList;
				} else {
					requestDatumList = datumList.subList(startIndex, endIndex);
				}
				PutMetricDataRequest request =
						new PutMetricDataRequest().withNamespace(nameSpace).withMetricData(requestDatumList);
				try {
					cloudWatchClient.putMetricData(request);
				} catch (Exception e) {
					throw new IOException("Could not publish metrics to CloudWatch", e);
				}
			}
		}
	}

	/**
	 * Set the credentials to use when publishing to AWS.
	 */
	// @NotRequired("either this or the cloudWatchClient needs to be set")
	public void setAwsCredentials(AWSCredentials awsCredentials) {
		this.awsCredentials = awsCredentials;
	}

	/**
	 * Set our application name which will be used to identify the namespace for the metric.
	 */
	// @Required
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Set to false to not add instance id to help identify the metric. Default is false.
	 */
	// @NotRequired("Default is true if running in EC2")
	public void setAddInstanceData(boolean addInstanceData) {
		this.addInstanceData = addInstanceData;
	}

	/**
	 * Sets the name-space prefix to use for metrics published from here.
	 */
	// @NotRequired("Default is " + DEFAULT_NAME_SPACE_PREFIX)
	public void setNameSpacePrefix(String nameSpacePrefix) {
		this.nameSpacePrefix = nameSpacePrefix;
	}

	/**
	 * Set the client to use to publish the metrics.
	 */
	// @NotRequired("Default is create one in initialize() with the credentials")
	public void setCloudWatchClient(AmazonCloudWatch cloudWatchClient) {
		this.cloudWatchClient = cloudWatchClient;
	}

	/**
	 * Return a map of metric name to associated data.
	 */
	private Map<String, List<MetricDatum>>
			buildMetricsMap(Map<ControlledMetric<?, ?>, MetricValueDetails> metricValues) {

		Map<String, List<MetricDatum>> metricMap = new HashMap<String, List<MetricDatum>>(metricValues.size());
		for (Map.Entry<ControlledMetric<?, ?>, MetricValueDetails> entry : metricValues.entrySet()) {

			ControlledMetric<?, ?> metric = entry.getKey();
			MetricValueDetails details = entry.getValue();

			List<MetricDatum> nameSpaceMetrics = metricMap.get(applicationName);
			if (nameSpaceMetrics == null) {
				nameSpaceMetrics = new ArrayList<MetricDatum>();
				metricMap.put(applicationName, nameSpaceMetrics);
			}

			double value = details.getValue().doubleValue();
			int numSamples = details.getNumSamples();
			double min = details.getMin().doubleValue();
			double max = details.getMax().doubleValue();

			// we do something special if it is an accumulator
			if (metric.getAggregationType() == AggregationType.SUM) {
				/*
				 * Looking at the ELB stats to see how AWS does it, if there were 100 requests in a minute then the
				 * value for each of them is a 1 and the number of samples for the minute was 100. Since our accumulator
				 * metrics report the value as the count, we need to flip the value and number-samples in the metric
				 * details that we are posting.
				 */
				long tmpValue = details.getValue().longValue();
				// unlikely but let's be careful out there
				if (tmpValue >= Integer.MAX_VALUE) {
					numSamples = Integer.MAX_VALUE;
				} else {
					numSamples = (int) tmpValue;
				}
				value = 1.0;
				min = 1.0;
				max = 1.0;
			}

			MetricDatum datum =
					new MetricDatum().withMetricName(metric.getName()).withUnit(convertUnit(metric.getUnit()));
			List<Dimension> dimensions = new ArrayList<Dimension>(3 /* max */);
			dimensions.add(new Dimension().withName(COMPONENT_DIMENSION).withValue(metric.getComponent()));
			if (metric.getModule() != null) {
				dimensions.add(new Dimension().withName(MODULE_DIMENSION).withValue(metric.getModule()));
			}
			datum.withDimensions(dimensions);

			// create a statisticSet or just a value
			if (numSamples == 1) {
				datum.withValue(value);
			} else {
				double sampleCount = numSamples;
				if (numSamples == 0) {
					/*
					 * Special case here, AWS does not allow a 0 sample count so we have to set it to be slightly more.
					 * See: http://stackoverflow.com/q/19696140
					 */
					sampleCount = ZERO_NUM_SAMPLES_REPLACEMENT;
				}
				// we do the multiplication here because CloudWatch wants a sum
				double valueSum = value * numSamples;
				datum.withStatisticValues(new StatisticSet().withMinimum(min)
						.withMaximum(max)
						.withSampleCount(sampleCount)
						.withSum(valueSum));
			}

			nameSpaceMetrics.add(datum);

			// copy our metric and add another one in with the instance-id
			if (instanceId != null) {
				MetricDatum instanceDatum = copyDatum(datum);
				dimensions.add(new Dimension().withName(INSTANCE_ID_DIMENSION).withValue(instanceId));
				instanceDatum.withDimensions(dimensions);
				nameSpaceMetrics.add(instanceDatum);
			}
		}

		return metricMap;
	}

	private MetricDatum copyDatum(MetricDatum datum) {
		MetricDatum copy = new MetricDatum().withMetricName(datum.getMetricName()).withUnit(datum.getUnit());
		Double datumValue = datum.getValue();
		if (datumValue == null) {
			copy.withStatisticValues(datum.getStatisticValues());
		} else {
			copy.withValue(datumValue);
		}
		return copy;
	}

	/**
	 * Convert the unit from the metric into one that CloudWatch likes.
	 */
	private StandardUnit convertUnit(String unitString) {
		if (unitString == null) {
			return StandardUnit.None;
		}
		String lowerUnitString = unitString.toLowerCase();
		StandardUnit unit = AWS_UNIT_MAP.get(lowerUnitString);
		if (unit == null) {
			return DEFAULT_AWS_UNIT;
		} else {
			return unit;
		}
	}

	/**
	 * Download the instance-id from AWS special IP which is available if running in EC2. We could have used a
	 * HttpClient but I didn't want to pay for the dependency.
	 */
	private static String downloadInstanceId(long connectTimeoutMillis) {
		Socket clientSocket = new Socket();
		Reader reader = null;
		Writer writer = null;
		try {
			clientSocket.connect(new InetSocketAddress(Inet4Address.getByName(AWS_INSTANCE_INFO_IP), 80), 1000);
			reader = new InputStreamReader(clientSocket.getInputStream());
			writer = new OutputStreamWriter(clientSocket.getOutputStream());
			writer.append("GET " + INSTANCE_ID_FETCH_PATH + " HTTP/1.1\r\n" //
					+ "Host: " + AWS_INSTANCE_INFO_IP + "\r\n" //
					+ "Connection: close\r\n\r\n");
			writer.flush();
			char[] buf = new char[1024];
			int numRead = reader.read(buf);
			String result = new String(buf, 0, numRead);
			Matcher matcher = INSTANCE_ID_RESULT_REGEX.matcher(result);
			if (matcher.matches()) {
				return matcher.group(1);
			} else {
				return "doesn't match";
			}
		} catch (IOException ioe) {
			// probably could not connect
			return null;
		} finally {
			MiscUtils.closeQuietly(reader);
			MiscUtils.closeQuietly(writer);
			MiscUtils.closeQuietly(clientSocket);
		}
	}
}
