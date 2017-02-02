package com.j256.simplemetrics.persister;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Publishes metrics to the log file on disk.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "com.j256", beanName = "TextFilePersister", folderNames = { "metrics" },
		description = "Text File Metrics Persister")
public class TextFileMetricsPersister implements MetricValuesPersister {

	private static final String NEWLINE = System.getProperty("line.separator");
	/**
	 * Default string that separates a metric from its value. This is exposed so the parser can use it.
	 */
	public static final String DEFAULT_SEPARATING_STRING = "=";

	private File outputDirectory;
	private String logFileNamePrefix;
	private boolean appendSysTimeMillis = true;
	private String separatingString = DEFAULT_SEPARATING_STRING;
	private boolean showDescription = false;

	private final AtomicLong dumpLogCount = new AtomicLong(0);
	private final AtomicLong cleanupLogCount = new AtomicLong(0);
	private long lastDumpTimeMillis;

	/**
	 * Dump a log file with all of the metrics into a log file.
	 */
	@Override
	public void persist(Map<ControlledMetric<?, ?>, Number> metricValues, long timeMillis) throws IOException {

		String logName = logFileNamePrefix;
		if (appendSysTimeMillis) {
			logName = logFileNamePrefix + timeMillis;
		}
		Writer writer = null;
		// write to a temp file
		File outputFile = new File(outputDirectory, logName + ".t");
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			for (Map.Entry<ControlledMetric<?, ?>, Number> entry : metricValues.entrySet()) {
				ControlledMetric<?, ?> metric = entry.getKey();
				if (showDescription) {
					writer.append("# ").append(metric.getDescription()).append(NEWLINE);
				}
				writer.append(MiscUtils.metricToString(metric));
				writer.append(separatingString);
				writer.append(entry.getValue().toString());
				writer.append(NEWLINE);
			}
		} catch (IOException e) {
			throw new IOException("Could not dump logfile to " + logName, e);
		} finally {
			MiscUtils.closeQuietly(writer);
		}

		// rename to our permanent name
		File destination = new File(outputDirectory, logName);
		outputFile.renameTo(destination);
		dumpLogCount.addAndGet(1);
		lastDumpTimeMillis = System.currentTimeMillis();
	}

	/**
	 * Delete the old metrics files that have not been modified in a certain number of milliseconds.
	 */
	public void cleanMetricFilesOlderThanMillis(long millisOld) {
		long tooOld = System.currentTimeMillis() - millisOld;
		for (File file : outputDirectory.listFiles()) {
			// if we have one of our old logfiles
			if (file.getName().startsWith(logFileNamePrefix) && file.lastModified() < tooOld) {
				if (file.delete()) {
					cleanupLogCount.incrementAndGet();
				}
			}
		}
	}

	/**
	 * Get the log file name prefix that we are writing.
	 */
	@JmxAttributeMethod(description = "File prefix we are writing")
	public String getLogFileNamePrefix() {
		return logFileNamePrefix;
	}

	/**
	 * Set the log file name prefix.
	 */
	// @Required
	public void setLogFileNamePrefix(String logFileNamePrefix) {
		this.logFileNamePrefix = logFileNamePrefix;
	}

	/**
	 * Are we appending the system time millis value to the name of the output file.
	 */
	@JmxAttributeMethod(description = "Whether we are appending the sys time millis to the output file")
	public boolean isAppendSysTimeMillis() {
		return appendSysTimeMillis;
	}

	/**
	 * Set to true to append the system time millis value to the name of the output file.
	 */
	// @NotRequired("Default is true")
	@JmxAttributeMethod(description = "Whether we are appending the sys time millis to the output file")
	public void setAppendSysTimeMillis(boolean appendSysTimeMillis) {
		this.appendSysTimeMillis = appendSysTimeMillis;
	}

	/**
	 * Directory where the output files will be written.
	 */
	@JmxAttributeMethod(description = "Directory where log files are written")
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * Set the directory where the output files will be written.
	 */
	// @Required
	public void setOutputDirectory(File outputDirectory) {
		outputDirectory.mkdirs();
		if (!outputDirectory.exists()) {
			throw new IllegalArgumentException("OutputDirectory " + outputDirectory + " does not exist");
		}
		if (!outputDirectory.isDirectory()) {
			throw new IllegalArgumentException("OutputDirectory is not directory: " + outputDirectory);
		}
		if (!outputDirectory.canWrite()) {
			throw new IllegalArgumentException("Can not write to OutputDirectory: " + outputDirectory);
		}
		this.outputDirectory = outputDirectory;
	}

	/**
	 * Set the separating string between the metric name and value. Default is "=".
	 */
	// @NotRequired("Default is " + DEFAULT_SEPARATING_STRING)
	public void setSeparatingString(String separatingString) {
		this.separatingString = separatingString;
	}

	/**
	 * Are we showing the description in the file on the previous line with a "# " prefix.
	 */
	@JmxAttributeMethod(description = "Show the description in the file")
	public boolean isShowDescription() {
		return showDescription;
	}

	/**
	 * Set to true to show the description in the file on the previous line with a "# " prefix.
	 */
	// @NotRequired("Default is false")
	@JmxAttributeMethod(description = "Show the description in the file")
	public void setShowDescription(boolean showDescription) {
		this.showDescription = showDescription;
	}

	/**
	 * Number of times the logs have been dumped to disk. 
	 */
	@JmxAttributeMethod(description = "Number of times we've written metrics")
	public long getDumpLogCount() {
		return dumpLogCount.get();
	}

	/**
	 * Number of times we have cleaned up old logs.
	 */
	@JmxAttributeMethod(description = "Number of times we've deleted metrics files")
	public long getCleanupLogCount() {
		return cleanupLogCount.get();
	}

	/**
	 * Get the last time we have dumped the metrics to disk.
	 */
	@JmxAttributeMethod(description = "Last time the metrics were written")
	public String getLastDumpTimeMillisString() {
		if (lastDumpTimeMillis == 0) {
			return "never";
		} else {
			return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").format(new Date(lastDumpTimeMillis));
		}
	}
}
