package com.j256.simplemetrics;

import java.io.Closeable;
import java.io.IOException;

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
}
