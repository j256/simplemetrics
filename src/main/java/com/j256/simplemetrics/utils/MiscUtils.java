package com.j256.simplemetrics.utils;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

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
	 * Close socket quietly.
	 */
	public static void closeQuietly(Socket socket) {
		if (socket != null) {
			try {
				socket.close();
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
	 * Capitalize the first character in the string if necessary.
	 */
	public static String capitalize(String str) {
		if (str == null) {
			return str;
		}
		int strLen = str.length();
		if (strLen == 0) {
			return str;
		}
		char first = Character.toTitleCase(str.charAt(0));
		if (first == str.charAt(0)) {
			return str;
		}
		return new StringBuilder(strLen).append(first).append(str.substring(1)).toString();
	}

	/**
	 * Return the name of the metric build by looking at the fields.
	 */
	public static String metricToString(ControlledMetric<?, ?> metric) {
		StringBuilder sb = new StringBuilder();
		sb.append(metric.getComponent());
		String mod = metric.getModule();
		if (mod != null) {
			sb.append('.').append(mod);
		}
		sb.append('.').append(metric.getName());
		return sb.toString();
	}
}
