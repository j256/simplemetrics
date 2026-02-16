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

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.utils.MiscUtils;

/**
 * Publishes metrics to the log file on disk.
 * 
 * @author graywatson
 */
public class TextFileMetricsPersister implements MetricValuesPersister {

	private static final String NEWLINE = System.getProperty("line.separator");
	/**
	 * Default string that separates a metric from its value. This is exposed so the parser can use it.
	 */
	public static final String DEFAULT_SEPARATING_STRING = "=";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");

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
		// write to a temp file
		File outputFile = new File(outputDirectory, logName + ".t");
		try (Writer writer = new BufferedWriter(new FileWriter(outputFile));) {
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
	public boolean isAppendSysTimeMillis() {
		return appendSysTimeMillis;
	}

	/**
	 * Set to true to append the system time millis value to the name of the output file.
	 */
	// @NotRequired("Default is true")
	public void setAppendSysTimeMillis(boolean appendSysTimeMillis) {
		this.appendSysTimeMillis = appendSysTimeMillis;
	}

	/**
	 * Directory where the output files will be written.
	 */
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
	public boolean isShowDescription() {
		return showDescription;
	}

	/**
	 * Set to true to show the description in the file on the previous line with a "# " prefix.
	 */
	// @NotRequired("Default is false")
	public void setShowDescription(boolean showDescription) {
		this.showDescription = showDescription;
	}

	/**
	 * Number of times the logs have been dumped to disk.
	 */
	public long getDumpLogCount() {
		return dumpLogCount.get();
	}

	/**
	 * Number of times we have cleaned up old logs.
	 */
	public long getCleanupLogCount() {
		return cleanupLogCount.get();
	}

	/**
	 * Get the last time we have dumped the metrics to disk.
	 */
	public String getLastDumpTimeMillisString() {
		if (lastDumpTimeMillis == 0) {
			return "never";
		} else {
			return ((SimpleDateFormat) DATE_FORMAT.clone()).format(new Date(lastDumpTimeMillis));
		}
	}
}
