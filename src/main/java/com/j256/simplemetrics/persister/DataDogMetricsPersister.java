package com.j256.simplemetrics.persister;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetric.AggregationType;
import com.j256.simplemetrics.metric.MetricValueDetails;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Class which persists metrics to the DataDog cloud monitoring service https://www.datadoghq.com/.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", folderNames = { "metrics" }, beanName = "DataDogPersister",
		description = "Persister for metrics to DataDog")
public class DataDogMetricsPersister implements MetricDetailsPersister, Closeable {

	private static final String DATADOG_API_VALIDATE_URL_KEY_PARAM = "api_key";
	private static final String DEFAULT_DATADOG_API_URL = "https://app.datadoghq.com/api/v1";
	private static final String DEFAULT_DATADOG_API_SERIES_URL = DEFAULT_DATADOG_API_URL + "/series";

	private static String[] METRIC_TAG_ARRAY = new String[] { "application:unknown" };

	private String apiKey;
	@JmxAttributeField(description = "URL where we are making our request")
	private String apiSeriesUrl = DEFAULT_DATADOG_API_SERIES_URL;
	@JmxAttributeField(description = "Name for this running application")
	private String applicationName = "unknown";
	@JmxAttributeField(description = "Name that this host calls itself or Inet4Address")
	private String localHostName;

	private static final Gson gson = new Gson();
	private static final Header CONTENT_TYPE_HEADER = new BasicHeader("Content-type", "application/json");
	private static final TrustManager laxHttpsTrustManager = new LaxTrustManager();

	private CloseableHttpClient httpClient;
	private String apiSeriesRequestUrl;

	/**
	 * Should be called if the no-arg construct is being used and after the file metrics have been set. Maybe by Springs
	 * init mechanism.
	 */
	public void initialize() throws IOException {
		if (localHostName == null) {
			localHostName = Inet4Address.getLocalHost().getHostName();
		}
		localHostName = protectDataDogString(localHostName);
		applicationName = protectDataDogString(applicationName);
		httpClient = createLaxSslHttpClient(5000);
		apiSeriesRequestUrl = apiSeriesUrl + "?" + DATADOG_API_VALIDATE_URL_KEY_PARAM + "=" + apiKey;
		// we add the name tag here to match how datadog publishes other AWS and instance data
		METRIC_TAG_ARRAY = new String[] { "application:" + applicationName, "name:" + this.localHostName };
	}

	@Override
	public void close() throws IOException {
		if (httpClient != null) {
			httpClient.close();
			httpClient = null;
		}
	}

	@Override
	public void persist(Map<ControlledMetric<?, ?>, MetricValueDetails> metricValueDetails, long timeCollectedMillis)
			throws IOException {

		List<DataDogMetric> dataDogMetrics = new ArrayList<DataDogMetric>();
		long timeSecs = timeCollectedMillis / 1000;
		for (Map.Entry<ControlledMetric<?, ?>, MetricValueDetails> entry : metricValueDetails.entrySet()) {
			ControlledMetric<?, ?> metric = entry.getKey();
			MetricValueDetails details = entry.getValue();
			dataDogMetrics.add(new DataDogMetric(metric, null, details.getValue(), timeSecs, localHostName));
			// if we have an average type of aggregation then add metrics for min/max
			if (metric.getAggregationType() == AggregationType.AVERAGE) {
				dataDogMetrics.add(new DataDogMetric(metric, "min", details.getMin(), timeSecs, localHostName));
				dataDogMetrics.add(new DataDogMetric(metric, "max", details.getMax(), timeSecs, localHostName));
			}
		}
		if (!dataDogMetrics.isEmpty()) {
			// post the metrics if we have them
			DataDogPost post = new DataDogPost(dataDogMetrics.toArray(new DataDogMetric[dataDogMetrics.size()]));
			postObject(post, apiSeriesRequestUrl);
		}
	}

	// @Required
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Use to set the instance server name for all metrics.
	 */
	// @NotRequired("Default is unknown")
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Use to set the local server name for all metrics.
	 */
	// @NotRequired("Default is the local hostname from Inet4Address
	public void setLocalHostName(String localHostName) {
		this.localHostName = localHostName;
	}

	/**
	 * Change any strange characters to be "_" per data-dog specs. See:
	 * https://help.datadoghq.com/hc/en-us/articles/203764715-What-are-valid-tags-
	 */
	private String protectDataDogString(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		boolean lastUnderscore = false;
		for (char ch : name.toCharArray()) {
			if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_'
					|| ch == '.' || ch == '-') {
				sb.append(Character.toLowerCase(ch));
				lastUnderscore = false;
			} else if (!lastUnderscore) {
				sb.append('_');
				// don't have multiple underscores in a row
				lastUnderscore = true;
			}
		}
		return sb.toString();
	}

	private void postObject(Object payload, String url) throws IOException {
		String json = gson.toJson(payload);

		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader(CONTENT_TYPE_HEADER);
		httpPost.setEntity(new StringEntity(json));

		CloseableHttpResponse response = httpClient.execute(httpPost);
		try {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_ACCEPTED) {
				throw new IOException("Could not publish metrics to DD: " + response);
			}
		} finally {
			MiscUtils.closeQuietly(response);
		}
	}

	private CloseableHttpClient createLaxSslHttpClient(int socketTimeoutMillis) throws IOException {
		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
		registryBuilder.register("http", new PlainConnectionSocketFactory());
		registryBuilder.register("https", createLaxHttpsSocketFactory());
		Registry<ConnectionSocketFactory> laxHttpsSchemeRegistry = registryBuilder.build();

		PoolingHttpClientConnectionManager laxConnectionManager =
				new PoolingHttpClientConnectionManager(laxHttpsSchemeRegistry);

		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		RequestConfig.Builder requestBuilder = RequestConfig.custom();
		requestBuilder = requestBuilder.setConnectTimeout(socketTimeoutMillis);
		requestBuilder = requestBuilder.setConnectionRequestTimeout(socketTimeoutMillis);
		requestBuilder = requestBuilder.setSocketTimeout(socketTimeoutMillis);
		clientBuilder.setDefaultRequestConfig(requestBuilder.build());
		clientBuilder.setConnectionManager(laxConnectionManager);
		return clientBuilder.build();
	}

	private ConnectionSocketFactory createLaxHttpsSocketFactory() throws IOException {
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("SSL");
			// set up a TrustManager that trusts everything
			sslContext.init(null, new TrustManager[] { laxHttpsTrustManager }, new SecureRandom());
		} catch (Exception e) {
			throw new IOException("problems working around SSL key issues", e);
		}
		return new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	}

	/**
	 * The top post object that wraps the metric post.
	 * 
	 * <pre>
	 *  { "series" : [
	 * 		{ "metric":"test.metric",
	 * 			"points":[[ epochTimeSeconds, value]],
	 * 			"unit": null,
	 * 			"type":"gauge",
	 *  		"host":"test.example.com",
	 * 		 	"tags":["environment:test"]
	 * 		}
	 * ]}
	 * </pre>
	 */
	private static class DataDogPost {
		@SerializedName("series")
		final DataDogMetric[] metrics;

		public DataDogPost(DataDogMetric[] metrics) {
			this.metrics = metrics;
		}
	}

	/**
	 * An individual "series" or metric we are posting.
	 */
	private static class DataDogMetric {

		private static final String TYPE_GUAGE = "gauge";
		private static final String TYPE_COUNTER = "counter";

		private static final Map<String, String> allowedUnitMap = new HashMap<String, String>();

		/*
		 * From datadog documentation:
		 * http://help.datadoghq.com/hc/en-us/articles/206780123-Which-units-can-I-use-for-my-metrics-in-the-metric-
		 * summary-page-
		 */
		private static final String[] ALLOWED_UNITS = new String[] { "bit", "byte", "kibibyte", "mebibyte", "gibibyte",
				"tebibyte", "pebibyte", "exbibyte", "microsecond", "millisecond", "second", "minute", "hour", "day",
				"week", "fraction", "percent", "connection", "request", "process", "file", "buffer", "inode", "sector",
				"block", "packet", "segment", "response", "message", "payload", "core", "thread", "table", "index",
				"lock", "transaction", "query", "row", "hit", "miss", "eviction", "dollar", "cent", "error", "host",
				"node", "key", "command", "offset", "page", "read", "write", "occurrence", "event", "time", "unit",
				"operation", "item", "record", "object", "cursor", "assertion", "fault", "percent_nano", };

		static {
			// first add the units directly
			for (String allowedUnit : ALLOWED_UNITS) {
				allowedUnitMap.put(allowedUnit, allowedUnit);
			}
			// then add them as plurals
			for (String allowedUnit : ALLOWED_UNITS) {
				String plural = allowedUnit + "s";
				if (!allowedUnitMap.containsKey(plural)) {
					allowedUnitMap.put(plural, allowedUnit);
				}
			}

			// name corrections
			allowedUnitMap.put("percentage", "percent");
		}

		@SerializedName("metric")
		final String name;
		// "gauge" or "counter";
		@SuppressWarnings("unused")
		final String type;
		@SuppressWarnings("unused")
		final String unit;
		// host-name
		@SuppressWarnings("unused")
		final String host;
		@SuppressWarnings("unused")
		final String[] tags;

		/**
		 * Metric points information. Unfortunately it is in the format of [[ 1446584717, 12.4 ], [ 1446584717, 12.5 ]]
		 * so we have to use a 2 dimensional object array instead of another class. Typically we only have one point of
		 * data in the array.
		 */
		@SerializedName("points")
		final Object[][] metricPoints;

		public DataDogMetric(ControlledMetric<?, ?> metric, String suffix, Number value, long timeMillis,
				String localHostName) {
			StringBuilder sb = new StringBuilder();
			sb.append(metric.getComponent());
			if (metric.getModule() != null) {
				sb.append('.').append(metric.getModule());
			}
			sb.append('.').append(metric.getName());
			if (suffix != null) {
				sb.append('.').append(suffix);
			}
			this.name = sb.toString();
			if (metric.getAggregationType() == AggregationType.SUM) {
				this.type = TYPE_COUNTER;
			} else {
				this.type = TYPE_GUAGE;
			}
			this.unit = unitConversion(metric.getUnit());
			this.host = localHostName;
			// this is necessary for the format, see above
			this.metricPoints = new Object[][] { new Object[] { timeMillis, value } };
			this.tags = METRIC_TAG_ARRAY;
		}

		/**
		 * Return the unit if it is allowed by datadog otherwise null.
		 */
		private String unitConversion(String unit) {
			return allowedUnitMap.get(unit);
		}
	}

	/**
	 * Allows us to ignore self-signed SSL certs.
	 */
	private static class LaxTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
			// noop
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
			// noop
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}
}
