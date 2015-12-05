package org.j256.simplemetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.j256.simplemetrics.ControlledMetric;
import com.j256.simplemetrics.ControlledMetricAccum;
import com.j256.simplemetrics.ControlledMetricValue;
import com.j256.simplemetrics.LogFileMetricsPersister;

public class LogFileMetricsPersisterTest {

	private final static String TEMP_DIR = "target/metricsManagerTest";

	@Before
	@After
	public void cleanTmp() {
		deleteFile(new File(TEMP_DIR));
	}

	private void deleteFile(File file) {
		File[] files = file.listFiles();
		if (files != null) {
			for (File subFile : files) {
				deleteFile(subFile);
			}
		}
		file.delete();
	}

	@Test(timeout = 10000)
	public void testDoAll() throws Exception {
		LogFileMetricsPersister persister = new LogFileMetricsPersister();
		String prefix = "log.";
		persister.setLogFileNamePrefix(prefix);
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		persister.setOutputDirectory(tmpDir);

		String label = "4233h32oh343242";
		String component = "comp";
		String model = "mod";
		ControlledMetricValue metric = new ControlledMetricValue(component, model, label, "desc", "unit");
		long value = 123213213;
		metric.adjustValue(value);
		long before = System.currentTimeMillis();
		assertEquals(0, persister.getDumpLogCount());
		persister.persist(metricValueMap(metric), System.currentTimeMillis());
		assertEquals(1, persister.getDumpLogCount());
		long after = System.currentTimeMillis();

		assertTrue(findEntry(prefix, component + "." + model + "." + label, before, after) != -1L);
	}

	private Map<ControlledMetric<?, ?>, Number> metricValueMap(ControlledMetric<?, ?>... metrics) {
		Map<ControlledMetric<?, ?>, Number> metricMap = new HashMap<ControlledMetric<?, ?>, Number>();
		for (ControlledMetric<?, ?> metric : metrics) {
			metricMap.put(metric, metric.getValueToPersist());
		}
		return metricMap;
	}

	@Test
	public void testGetOutputDirectory() {
		LogFileMetricsPersister persister = new LogFileMetricsPersister();
		File tmpDir = new File(TEMP_DIR);
		persister.setOutputDirectory(tmpDir);
		assertEquals(tmpDir, persister.getOutputDirectory());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetOutputDirectoryFile() throws Exception {
		LogFileMetricsPersister manager = new LogFileMetricsPersister();
		File tmpDir = new File(TEMP_DIR);
		try {
			assertTrue(tmpDir.createNewFile());
			manager.setOutputDirectory(tmpDir);
		} finally {
			tmpDir.delete();
		}
	}

	@Test
	public void testGetFileFormat() {
		LogFileMetricsPersister manager = new LogFileMetricsPersister();
		String prefix = "log.";
		manager.setLogFileNamePrefix(prefix);
		assertEquals(prefix, manager.getLogFileNamePrefix());
	}

	@Test
	public void testRotatePeriods() throws Exception {
		LogFileMetricsPersister persister = new LogFileMetricsPersister();
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		persister.setOutputDirectory(tmpDir);
		String prefix = "log.";
		persister.setLogFileNamePrefix(prefix);

		String label = "big_value";
		ControlledMetricValue metric = new ControlledMetricValue("comp", "mod", label, "desc", "unit");

		String ext = "_per_something";

		long value = 48233232432L;
		metric.adjustValue(value);
		long before = System.currentTimeMillis();
		persister.persist(metricValueMap(metric), System.currentTimeMillis());
		long after = System.currentTimeMillis();
		assertEquals(-1L, findEntry(prefix, label + ext, before, after));
	}

	@Test
	public void testSetRotatePeriodsMetricAccum() throws Exception {
		LogFileMetricsPersister persister = new LogFileMetricsPersister();
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		persister.setOutputDirectory(tmpDir);
		String prefix = "log.";
		persister.setLogFileNamePrefix(prefix);

		String label = "big_value";
		ControlledMetricAccum metric = new ControlledMetricAccum("comp", "mod", label, "desc", "unit");

		String ext = "_per_something";

		long value = 48233232432L;
		metric.add(value);
		long before = System.currentTimeMillis();
		persister.persist(metricValueMap(metric), System.currentTimeMillis());
		long after = System.currentTimeMillis();
		assertEquals(-1L, findEntry(prefix, label + ext, before, after));
	}

	@Test
	public void testSetRotatePeriodsAffectMetrics() throws Exception {
		LogFileMetricsPersister persister = new LogFileMetricsPersister();
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		persister.setOutputDirectory(tmpDir);
		String prefix = "log.";
		persister.setLogFileNamePrefix(prefix);

		String label = "big_value";
		String component = "comp";
		String model = "mod";
		ControlledMetricValue metric = new ControlledMetricValue(component, model, label, "desc", "unit");
		long duration = 500;
		int numSamples = 10;

		String ext = "per_something";

		long value = 48233232432L;
		metric.adjustValue(value);
		Thread.sleep(1 + duration / numSamples);
		long before = System.currentTimeMillis();
		persister.persist(metricValueMap(metric), System.currentTimeMillis());
		long after = System.currentTimeMillis();
		assertTrue(findEntry(prefix, component + "." + model + "." + label + "." + ext, before, after) != value);
	}

	@Test
	public void testCleanup() throws Exception {
		LogFileMetricsPersister persister = new LogFileMetricsPersister();
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		persister.setOutputDirectory(tmpDir);
		String prefix = "log.";
		persister.setLogFileNamePrefix(prefix);

		String label = "4233h32oh343242";
		String component = "comp";
		String model = "mod";
		ControlledMetricValue metric = new ControlledMetricValue(component, model, label, "desc", "unit");

		long value = 123213213;
		metric.adjustValue(value);

		long before = System.currentTimeMillis();
		assertEquals(0, persister.getDumpLogCount());
		persister.persist(metricValueMap(metric), System.currentTimeMillis());
		long after = System.currentTimeMillis();
		assertEquals(1, persister.getDumpLogCount());

		assertEquals(value, findEntry(prefix, component + "." + model + "." + label, before, after));

		assertEquals(0, persister.getCleanupLogCount());
		persister.cleanMetricFilesOlderThanMillis(5000);
		assertEquals(0, persister.getCleanupLogCount());
		// needs to be >1 second to overcome filesystem modified resolution
		Thread.sleep(1001);
		persister.cleanMetricFilesOlderThanMillis(100);
		assertEquals(1, persister.getCleanupLogCount());

		assertEquals(-1, findEntry(prefix, component + "." + model + "." + label, before, after));
	}

	private long findEntry(String prefix, String label, long fromTime, long toTime) throws Exception {
		for (long time = fromTime; time <= toTime; time++) {
			File logFile = new File(new File(TEMP_DIR), prefix + Long.toString(time));
			if (logFile.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(logFile));
				try {
					while (true) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
						if (line.contains(label)) {
							String[] fields = line.split("=", 2);
							if (fields[0].equals(label)) {
								return Long.parseLong(fields[1]);
							}
						}
					}
				} finally {
					reader.close();
				}
			}
		}
		return -1;
	}
}
