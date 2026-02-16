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

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetric.AggregationType;
import com.j256.simplemetrics.metric.MetricValueDetails;
import com.j256.simplemetrics.utils.MiscUtils;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum.Builder;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

/**
 * Class which persists our metrics to Amazon's AWS CloudWatch cloud service. This requires the aws-java-sdk package to
 * be in the classpath to compile.
 * 
 * <p>
 * <b>NOTE:</b> If you are using the no-arg constructor (like with Spring) you will need to make sure that
 * {@link #initialize()} is called.
 * </p>
 */
public class CloudWatchMetricsPersister implements MetricDetailsPersister {

	private static final String DEFAULT_NAME_SPACE_PREFIX = "Application";
	private static final String INSTANCE_ID_DIMENSION = "InstanceId";
	private static final String COMPONENT_DIMENSION = "Component";
	private static final String MODULE_DIMENSION = "Module";
	static final int MAX_NUM_DATUM_ALLOWED_PER_POST = 20;
	private static final StandardUnit DEFAULT_AWS_UNIT = StandardUnit.COUNT;
	static final double ZERO_NUM_SAMPLES_REPLACEMENT = 0.000000001D;

	private static final String AWS_INSTANCE_INFO_IP = "169.254.169.254";
	private static final String INSTANCE_ID_FETCH_PATH = "/latest/meta-data/instance-id";
	private static final int AWS_CONNECT_TIMEOUT_MILLIS = 2000;

	// makes sure that we are running in EC2 and extracts the instance-id name
	private static final Pattern INSTANCE_ID_RESULT_REGEX = Pattern.compile("(?s).*Server: EC2ws.*\r?\n\r?\n(i-.*)");
	private static final Map<String, StandardUnit> AWS_UNIT_MAP = new HashMap<String, StandardUnit>();

	private String applicationName = "unknown";
	private String nameSpacePrefix = DEFAULT_NAME_SPACE_PREFIX;
	private boolean addInstanceData = true;

	private CloudWatchClient cloudWatchClient;
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
		AWS_UNIT_MAP.put("kb", StandardUnit.KILOBYTES);
		AWS_UNIT_MAP.put("mb", StandardUnit.MEGABYTES);
		AWS_UNIT_MAP.put("gb", StandardUnit.GIGABYTES);
		AWS_UNIT_MAP.put("mbit", StandardUnit.MEGABITS);
		AWS_UNIT_MAP.put("millis", StandardUnit.MILLISECONDS);
		AWS_UNIT_MAP.put("ms", StandardUnit.MILLISECONDS);
		AWS_UNIT_MAP.put("msec", StandardUnit.MILLISECONDS);
		AWS_UNIT_MAP.put("msecs", StandardUnit.MILLISECONDS);
		AWS_UNIT_MAP.put("bps", StandardUnit.BYTES_SECOND);
		AWS_UNIT_MAP.put("%", StandardUnit.PERCENT);
		AWS_UNIT_MAP.put("percentage", StandardUnit.PERCENT);
		AWS_UNIT_MAP.put("count", StandardUnit.COUNT);
		AWS_UNIT_MAP.put("many", StandardUnit.COUNT);
	}

	public CloudWatchMetricsPersister() {
		// for spring
	}

	public CloudWatchMetricsPersister(String applicationName, boolean addInstanceData) {
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
			cloudWatchClient = CloudWatchClient.builder().build();
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
						PutMetricDataRequest.builder().namespace(nameSpace).metricData(requestDatumList).build();
				try {
					cloudWatchClient.putMetricData(request);
				} catch (Exception e) {
					throw new IOException("Could not publish metrics to CloudWatch", e);
				}
			}
		}
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
	 * For testing purposes.
	 */
	// @NotRequired("Default is create one in initialize() with the credentials")
	public void setCloudWatchClient(CloudWatchClient cloudWatchClient) {
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

			Builder datumBuilder =
					MetricDatum.builder().metricName(metric.getName()).unit(convertUnit(metric.getUnit()));
			List<Dimension> dimensions = new ArrayList<Dimension>(3 /* max */);
			dimensions.add(Dimension.builder().name(COMPONENT_DIMENSION).value(metric.getComponent()).build());
			if (metric.getModule() != null) {
				dimensions.add(Dimension.builder().name(MODULE_DIMENSION).value(metric.getModule()).build());
			}
			datumBuilder.dimensions(dimensions);

			// create a statisticSet or just a value
			if (numSamples == 1) {
				datumBuilder.value(value);
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
				datumBuilder.statisticValues(StatisticSet.builder()
						.minimum(min)
						.maximum(max)
						.sampleCount(sampleCount)
						.sum(valueSum)
						.build());
			}

			MetricDatum datum = datumBuilder.build();
			nameSpaceMetrics.add(datum);

			// copy our metric and add another one in with the instance-id
			if (instanceId != null) {
				MetricDatum.Builder instanceDatumBuilder = copyDatum(datum);
				dimensions.add(Dimension.builder().name(INSTANCE_ID_DIMENSION).value(instanceId).build());
				instanceDatumBuilder.dimensions(dimensions);
				nameSpaceMetrics.add(instanceDatumBuilder.build());
			}
		}

		return metricMap;
	}

	private MetricDatum.Builder copyDatum(MetricDatum datum) {
		MetricDatum.Builder copyBuilder = MetricDatum.builder().metricName(datum.metricName()).unit(datum.unit());
		Double datumValue = datum.value();
		if (datumValue == null) {
			copyBuilder.statisticValues(datum.statisticValues());
		} else {
			copyBuilder.value(datumValue);
		}
		return copyBuilder;
	}

	/**
	 * Convert the unit from the metric into one that CloudWatch likes.
	 */
	private StandardUnit convertUnit(String unitString) {
		if (unitString == null) {
			return StandardUnit.NONE;
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
	private static String downloadInstanceId(int connectTimeoutMillis) {
		try (Socket clientSocket = new Socket();) {
			clientSocket.connect(new InetSocketAddress(Inet4Address.getByName(AWS_INSTANCE_INFO_IP), 80),
					connectTimeoutMillis);
			try (Reader reader = new InputStreamReader(clientSocket.getInputStream());
					Writer writer = new OutputStreamWriter(clientSocket.getOutputStream());) {
				writer.append("GET " + INSTANCE_ID_FETCH_PATH + " HTTP/1.1\r\n" //
						+ "Host: " + AWS_INSTANCE_INFO_IP + "\r\n" //
						+ "Connection: close\r\n\n");
				writer.flush();

				// read in the whole server response including headers
				char[] buf = new char[1024];
				int numRead = reader.read(buf);
				String result = new String(buf, 0, numRead);
				Matcher matcher = INSTANCE_ID_RESULT_REGEX.matcher(result);
				if (matcher.matches()) {
					return matcher.group(1);
				} else {
					return null;
				}
			}
		} catch (IOException ioe) {
			// probably could not connect
			return null;
		}
	}
}
