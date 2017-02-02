package com.j256.simplemetrics.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricAccum;
import com.j256.simplemetrics.metric.ControlledMetricValue;

/**
 * Metric read from a file on the file-system. Often used to read from the /proc file-system under Linux. You can use
 * regular-expression, line prefix string, or line-number to specify which line from the file that you want to process.
 * In terms of extracting the value from fields, you can use a regular-expression to split the line into columns and
 * then set the column that you want. If you are matching the line with a regular-expression, the column number will
 * correspond to a () group-number.
 * 
 * <p>
 * You can also adjust the extracted value using an adjustment operation and value. For example, the value in the file
 * might be the number of 512 byte sectors when you really want the metric to reflect bytes. You could then use the
 * {@link FileMetricOperation#MULTIPLY} adjustment-operation and an adjustment-value of 512.
 * </p>
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
	private FileMetricKind kind;
	private int column = -1;
	private Pattern splitPattern = Pattern.compile(" ");
	private Pattern linePattern;
	private String prefix;
	private double adjustmentValue;
	private FileMetricOperation adjustmentOperation;
	private int lineNumber;

	public FileMetric() {
		// for spring
	}

	public FileMetric(String metricName, String metricComponent, String metricModule, String description,
			File metricFile, FileMetricKind kind, int column, String lineSplit, String prefix)
			throws IllegalArgumentException {
		this.metricName = metricName;
		this.metricComponent = metricComponent;
		this.metricModule = metricModule;
		this.description = description;
		this.metricFile = metricFile;
		this.kind = kind;
		this.column = column;
		this.splitPattern = Pattern.compile(lineSplit);
		this.prefix = prefix;
		initialize();
	}

	/**
	 * Should be called after all of the setter methods are called, maybe my Spring init mechanism?
	 */
	public void initialize() throws IllegalArgumentException {
		if (metricFile == null) {
			throw new IllegalArgumentException(
					"metricFile was not specified for " + getClass() + " name " + metricName);
		} else if (!metricFile.exists()) {
			if (required) {
				throw new IllegalArgumentException(
						"metricFile " + metricFile + " does not exist for " + getClass() + " name " + metricName);
			} else {
				return;
			}
		}
		if (kind == null) {
			throw new IllegalArgumentException("kind was not specified for " + getClass() + " name " + metricName);
		}
		switch (kind) {
			case DIR:
				if (!metricFile.isDirectory()) {
					throw new IllegalArgumentException("metricFile " + metricFile + " is not a directory for "
							+ this.getClass() + " name " + metricName);
				}
				metric = new ControlledMetricValue(metricComponent, metricModule, metricName, description, unit);
				break;
			case FILE_ACCUM:
				if (column < 0) {
					throw new IllegalArgumentException(
							"metric " + this.getClass() + " name " + metricName + " did not specify column value");
				}
				metric = new ControlledMetricAccum(metricComponent, metricModule, metricName, description, unit);
				break;
			case FILE_VALUE:
				if (column < 0) {
					throw new IllegalArgumentException(
							"metric " + this.getClass() + " name " + metricName + " did not specify column value");
				}
				metric = new ControlledMetricValue(metricComponent, metricModule, metricName, description, unit);
				break;
			default:
				throw new IllegalArgumentException("unknown kind " + kind + " for file metric with name " + metricName);
		}
		initialized = true;
	}

	/**
	 * Update the value in the metric.
	 */
	public void updateValue() throws IOException {
		switch (kind) {
			case DIR:
				long longValue = metricFile.list().length;
				if (adjustmentOperation == null) {
					metric.adjustValue(longValue);
				} else {
					metric.adjustValue(adjustValue(longValue));
				}
				break;
			case FILE_ACCUM:
			case FILE_VALUE:
				double doubleValue = extractNumberFromFile();
				if (adjustmentOperation == null) {
					metric.adjustValue(doubleValue);
				} else {
					metric.adjustValue(adjustValue(doubleValue));
				}
				break;
			default:
				throw new IllegalArgumentException(
						"unknown kind " + kind + " for file metric with label " + metricName);
		}
	}

	public ControlledMetric<?, ?> getMetric() {
		return metric;
	}

	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * The file that we process to find the value. Either this or {@link #setMetricFiles(String[])} must be specified.
	 */
	// @NotRequired("Either this or the metric-files is required however")
	public void setMetricFile(String metricFile) {
		this.metricFile = new File(metricFile);
	}

	/**
	 * An array of files that that will be looked at. The first one that exists will be used. Either this or
	 * {@link #setMetricFile(String)} must be specified.
	 */
	// @NotRequired("Either this or the metric-files is required however")
	public void setMetricFiles(String[] metricFiles) {
		for (String fileName : metricFiles) {
			File metricFile = new File(fileName);
			if (metricFile.exists()) {
				this.metricFile = metricFile;
				break;
			}
		}
	}

	/**
	 * The name to be associated with the metric. See {@link ControlledMetric#getName()}.
	 */
	// @Required
	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	/**
	 * The component to be associated with the metric. See {@link ControlledMetric#getComponent()}.
	 */
	// @Required
	public void setMetricComponent(String metricComponent) {
		this.metricComponent = metricComponent;
	}

	/**
	 * The module to be associated with the metric. See {@link ControlledMetric#getModule()}.
	 */
	// @NotRequired("Default is none")
	public void setMetricModule(String metricModule) {
		this.metricModule = metricModule;
	}

	/**
	 * The description to be associated with the metric. See {@link ControlledMetric#getDescription()}.
	 */
	// @Required
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * The unit to be associated with the metric. See {@link ControlledMetric#getUnit()}.
	 */
	// @NotRequired("optional")
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * The kind of the metric. See {@link FileMetricKind}.
	 */
	public void setKind(FileMetricKind kind) {
		this.kind = kind;
	}

	/**
	 * The column from a split perspective (0 to N-1) or the matcher.group(column) if using the line-pattern regex. With
	 * the regex, the 0th group is the entire matched string and the 1st group is the first () in the pattern.
	 */
	// @NotRequired("No column specified is the default")
	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * Regex string to use to divide the line up into fields so we can use the column to extract one of the fields.
	 * Default is a single space.
	 */
	// @NotRequired("Can use this or a pattern matcher")
	public void setLineSplit(String lineSplit) {
		this.splitPattern = Pattern.compile(lineSplit);
	}

	/**
	 * Set the regex pattern that should match the line. The column then becomes the group-number from the regex
	 * matcher.
	 */
	public void setLinePattern(String linePattern) {
		this.linePattern = Pattern.compile(linePattern);
	}

	/**
	 * Prefix string to look for in the file. If this is set then the line won't match unless this prefix is seen.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Set whether or not this metric is required, default is false. If the file doesn't exist on this system and it is
	 * required then this will cause an exception to be thrown.
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}

	/**
	 * If you need to adjust the extracted value at all, you can use this adjustment combined with the
	 * {@link #setAdjustmentOperation(FileMetricOperation)} method to set the operation.
	 */
	public void setAdjustmentValue(double adjustmentValue) {
		this.adjustmentValue = adjustmentValue;
	}

	/**
	 * If you need to adjust the extracted value, this sets the operation that will be applied with the
	 * {@link #setAdjustmentValue(double)}.
	 */
	public void setAdjustmentOperation(FileMetricOperation adjustmentOperation) {
		this.adjustmentOperation = adjustmentOperation;
	}

	/**
	 * Number of the line in the file to process. The first line in the file is #1 not 0.
	 */
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Override
	public String toString() {
		return MiscUtils.metricToString(metric);
	}

	private double extractNumberFromFile() throws IOException {
		String line;
		BufferedReader reader = null;
		Matcher matcher = null;
		try {
			reader = new BufferedReader(new FileReader(metricFile));
			int lineCount = 0;
			while (true) {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				lineCount++;
				// check our line number
				if (lineNumber > 0) {
					if (lineCount == lineNumber) {
						break;
					} else {
						continue;
					}
				}
				if (prefix != null) {
					if (!line.startsWith(prefix)) {
						continue;
					}
				}
				if (linePattern == null) {
					// prefix is null or this line starts with it
					break;
				}
				// try our line pattern
				matcher = linePattern.matcher(line);
				if (matcher.matches()) {
					break;
				}
				matcher = null;
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
				throw new IOException(
						"Prefix " + prefix + " not found for metric " + metricName + " from file " + metricFile);
			}
		}

		String value;
		if (matcher == null) {
			String[] columns = splitPattern.split(line);
			if (column >= columns.length) {
				throw new IOException("Column " + column + " more than split size " + columns.length + " in metrics "
						+ metricName + " in file " + metricFile);
			}
			value = columns[column];
		} else {
			if (column > matcher.groupCount()) {
				throw new IOException("Column " + column + " more than line pattern column-count "
						+ matcher.groupCount() + " in metrics " + metricName + " in file " + metricFile);
			}
			value = matcher.group(column);
			if (value == null) {
				throw new IOException(
						"Column " + column + " did not match line in metrics " + metricName + " in file " + metricFile);
			}
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new IOException("Invalid number '" + value + "' in metrics " + metricName + " in file " + metricFile
					+ " column " + column);
		}
	}

	private Number adjustValue(long value) {
		switch (adjustmentOperation) {
			case ADD:
				return value + adjustmentValue;
			case SUBTRACT:
				return value - adjustmentValue;
			case MULTIPLY:
				return value * adjustmentValue;
			case DIVIDE:
				if (adjustmentValue == 0.0D) {
					return Double.MAX_VALUE;
				} else {
					return value / adjustmentValue;
				}
			default:
				return value;
		}
	}

	private double adjustValue(double value) {
		switch (adjustmentOperation) {
			case ADD:
				return value + adjustmentValue;
			case SUBTRACT:
				return value - adjustmentValue;
			case MULTIPLY:
				return value * adjustmentValue;
			case DIVIDE:
				if (adjustmentValue == 0.0D) {
					return Double.MAX_VALUE;
				} else {
					return value / adjustmentValue;
				}
			default:
				return value;
		}
	}

	/**
	 * Kind of metrics that we are processing here.
	 */
	public enum FileMetricKind {
		/** count of the number of entries in the directory */
		DIR,
		/** number which accumulates */
		FILE_ACCUM,
		/** number which is a value */
		FILE_VALUE,
		// end
		;
	}

	/**
	 * Adjustment operations that can be used to adjust the extracted value by using
	 * {@link FileMetric#setAdjustmentOperation(FileMetricOperation)}.
	 */
	public enum FileMetricOperation {
		/** value added to adjustment */
		ADD,
		/** value subtracted by adjustment */
		SUBTRACT,
		/** value multiplied by adjustment */
		MULTIPLY,
		/** value divided by adjustment */
		DIVIDE,
		// end
		;
	}
}
