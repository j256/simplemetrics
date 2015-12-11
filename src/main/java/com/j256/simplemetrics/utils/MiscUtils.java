package com.j256.simplemetrics.utils;

import java.io.Closeable;
import java.io.IOException;

import com.j256.simplemetrics.metric.ControlledMetric;

/**
 * Set of common utility methods copied from the Net.
 * 
 * @author graywatson
 */
public class MiscUtils {

	/**
	 * Close something quietly.
	 */
	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ioe) {
				// ignore
			}
		}
	}

	/**
	 * Return true if the string is null, empty, or all whitespace, otherwise false.
	 */
	public static boolean isBlank(CharSequence cs) {
		if (cs == null || cs.length() == 0) {
			return true;
		}
		for (int i = 0; i < cs.length(); i++) {
			if (!Character.isWhitespace(cs.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return the name of the metric build by looking at the fields.
	 */
	public static String metricToString(ControlledMetric<?, ?> metric) {
		StringBuilder sb = new StringBuilder();
		sb.append(metric.getComponent());
		if (metric.getModule() != null) {
			sb.append('.');
			sb.append(metric.getModule());
		}
		sb.append('.');
		sb.append(metric.getName());
		return sb.toString();
	}
}
