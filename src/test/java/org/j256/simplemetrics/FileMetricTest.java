package org.j256.simplemetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.j256.simplemetrics.FileMetric;
import com.j256.simplemetrics.FileMetric.ProcMetricKind;

public class FileMetricTest {

	private static final String PROC_PREFIX = "target/test-classes/proc";

	@Test
	public void testFileMetric() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test
	public void testFileMetricAccum() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test(expected = IOException.class)
	public void testFileMetricMissingPrefix() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Unknown-prefix:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test(expected = IOException.class)
	public void testFileMetricEmptyFile() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/emptyFile");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test(expected = IOException.class)
	public void testInvalidColumn() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.setColumn(100);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test(expected = IOException.class)
	public void testInvalidNumberFormat() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		// invalid column "kB"
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test
	public void testDirMetric() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.DIR);
		metric.setMetricFile(PROC_PREFIX + "/self/fd");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDirNotDir() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.DIR);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isEnabled());
		metric.updateValue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoFile() throws Exception {
		FileMetric metric = new FileMetric();
		metric.initialize();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoKind() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.initialize();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoColumn() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.initialize();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoColumnAccum() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setKind(ProcMetricKind.COLUMN_ACCUM);
		metric.initialize();
	}

	@Test
	public void testIsNotEnabled() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/does-not-exist");
		metric.initialize();
		assertFalse(metric.isEnabled());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsNotEnabledThrow() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(ProcMetricKind.COLUMN_VALUE);
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/does-not-exist");
		metric.setRequired(true);
		metric.initialize();
		assertEquals(label, metric.getMetric().getName());
		assertFalse(metric.isEnabled());
	}
}
