package com.j256.simplemetrics.publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.j256.simplemetrics.MiscUtils;
import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;

/**
 * Metric read from a file on the file-system. Often used to read from the /proc file-system under Linux.
 * 
 * @author graywatson
 */
public class FileMetric {

	private boolean required = false;
	private boolean initialized = false;
	private File metricFile;
	private String metricName;
	private String metricComponent;
	private String metricModule;
	private String description;
	private String unit;
	private ControlledMetric<?, ?> metric;
	private ProcMetricKind kind;
	private int column = -1;
	private String lineSplit = " ";
	private String prefix;

	public FileMetric() {
		// for spring
	}

	public FileMetric(String metricName, String metricComponent, String metricModule, String description,
			File metricFile, ProcMetricKind kind, int column, String lineSplit, String prefix)
			throws IllegalArgumentException {
		this.metricName = metricName;
		this.metricComponent = metricComponent;
		this.metricModule = metricModule;
		this.description = description;
		this.metricFile = metricFile;
		this.kind = kind;
		this.column = column;
		this.lineSplit = lineSplit;
		this.prefix = prefix;
		initialize();
	}

	/**
	 * Should be called after all of the setter methods are called, maybe my Spring init mechanism?
	 */
	public void initialize() throws IllegalArgumentException {
		if (metricFile == null) {
			throw new IllegalArgumentException("metricFile was not specified for " + getClass() + " name " + metricName);
		} else if (!metricFile.exists()) {
			if (required) {
				throw new IllegalArgumentException("metricFile " + metricFile + " does not exist for " + getClass()
						+ " name " + metricName);
			} else {
				return;
			}
		}
		if (kind == null) {
			throw new IllegalArgumentException("kind was not specified for " + getClass() + " name " + metricName);
		}
		switch (kind) {
			case DIR :
				if (!metricFile.isDirectory()) {
					throw new IllegalArgumentException("metricFile " + metricFile + " is not a directory for "
							+ this.getClass() + " name " + metricName);
				}
				metric = new ControlledMetricValue(metricComponent, metricModule, metricName, description, unit);
				break;
			case COLUMN_ACCUM :
				if (column < 0) {
					throw new IllegalArgumentException("metric " + this.getClass() + " name " + metricName
							+ " did not specify column value");
				}
				metric = new ControlledMetricAccum(metricComponent, metricModule, metricName, description, unit);
				break;
			case COLUMN_VALUE :
				if (column < 0) {
					throw new IllegalArgumentException("metric " + this.getClass() + " name " + metricName
							+ " did not specify column value");
				}
				metric = new ControlledMetricValue(metricComponent, metricModule, metricName, description, unit);
				break;
			default :
				throw new IllegalArgumentException("unknown kind " + kind + " for proc metric with name " + metricName);
		}
		initialized = true;
	}

	/**
	 * Update the value in the metric.
	 */
	public void updateValue() throws IOException {
		switch (kind) {
			case DIR :
				metric.adjustValue(metricFile.list().length);
				break;
			case COLUMN_ACCUM :
			case COLUMN_VALUE :
				metric.adjustValue(extractNumberFromFile());
				break;
			default :
				throw new IllegalArgumentException("unknown kind " + kind + " for proc metric with label " + metricName);
		}
	}

	public ControlledMetric<?, ?> getMetric() {
		return metric;
	}

	public boolean isInitialized() {
		return initialized;
	}

	// @Required
	public void setMetricFile(String metricFile) {
		this.metricFile = new File(metricFile);
	}

	// @Required
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	// @Required
	public void setMetricComponent(String metricComponent) {
		this.metricComponent = metricComponent;
	}

	// @NotRequired("Default is none")
	public void setMetricModule(String metricModule) {
		this.metricModule = metricModule;
	}

	// @Required
	public void setDescription(String description) {
		this.description = description;
	}

	// @NotRequired("optional")
	public void setUnit(String unit) {
		this.unit = unit;
	}

	public void setKind(ProcMetricKind kind) {
		this.kind = kind;
	}

	// @NotRequired("No column specified is the default")
	public void setColumn(int column) {
		this.column = column;
	}

	public void setLineSplit(String lineSplit) {
		this.lineSplit = lineSplit;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	@Override
	public String toString() {
		return metric.toString();
	}

	private Double extractNumberFromFile() throws IOException {
		String line;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(metricFile));
			while (true) {
				line = reader.readLine();
				if (line == null || prefix == null || line.startsWith(prefix)) {
					break;
				}
			}
		} catch (IOException e) {
			throw new IOException("Problems reading metric " + metricName + " from file " + metricFile, e);
		} finally {
			MiscUtils.closeQuietly(reader);
		}
		if (line == null) {
			if (prefix == null) {
				throw new IOException("No line read for metric " + metricName + " from file " + metricFile);
			} else {
				throw new IOException("Prefix " + prefix + " not found for metric " + metricName + " from file "
						+ metricFile);
			}
		}
		String[] columns = line.split(lineSplit);
		if (column >= columns.length) {
			throw new IOException("Column " + column + " more than split size " + columns.length + " in metrics "
					+ metricName + " in file " + metricFile);
		}
		try {
			return Double.parseDouble(columns[column]);
		} catch (NumberFormatException e) {
			throw new IOException("Invalid number " + columns[column] + " in metrics " + metricName + " in file "
					+ metricFile + " column " + column);
		}
	}

	/**
	 * King of metric that we are processing here.
	 */
	public enum ProcMetricKind {
		// could of the number of entries in the directory
		DIR,
		// number which accumulates
		COLUMN_ACCUM,
		// number which is a value
		COLUMN_VALUE,
		// end
		;
	}
}
