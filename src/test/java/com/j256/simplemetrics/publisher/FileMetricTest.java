package com.j256.simplemetrics.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.j256.simplemetrics.utils.FileMetric;
import com.j256.simplemetrics.utils.FileMetric.FileMetricKind;
import com.j256.simplemetrics.utils.FileMetric.FileMetricOperation;

public class FileMetricTest {

	private static final String PROC_PREFIX = "target/test-classes/proc";
	private final String TEMP_DIR = "target/" + getClass().getSimpleName();

	@Before
	@After
	public void cleanTmp() {
		deleteFile(new File(TEMP_DIR));
	}

	@Test
	public void testFileMetric() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test
	public void testFileMetricNoPrefix() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(3);
		metric.setMetricFile(PROC_PREFIX + "/disk/stat");
		metric.setLineSplit("\\s+");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(13024394, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testMultipleFilesSpecified() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFiles(new String[] { "some-file-that-doesnt-exist", PROC_PREFIX + "/meminfo" });
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test
	public void testFileMetricAccum() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test
	public void testDecimalMetric() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.setDecimalNumber(true);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(4385684, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testUnknownMetricFile() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setMetricName("foo");
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.setRequired(false);
		metric.initialize();
	}

	@Test
	public void testValueDiffMetric() throws Exception {
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		File file = new File(tmpDir, "metricFile");
		FileMetric metric = new FileMetric();
		long value1 = 13413123123L;
		writeToFile(file, value1 + "\n");
		metric.setMetricName("foo");
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE_DIFF);
		metric.setColumn(0);
		metric.setMetricFile(file.getCanonicalPath());
		metric.setDecimalNumber(true);
		metric.initialize();
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(0, metric.getMetric().getValue().longValue());
		long diff = 10;
		long value2 = value1 + diff;
		writeToFile(file, value2 + "\n");
		metric.updateValue();
		assertEquals(diff, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAccumDiffMetric() throws Exception {
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		File file = new File(tmpDir, "metricFile");
		FileMetric metric = new FileMetric();
		long value1 = 13413123123L;
		writeToFile(file, value1 + "\n");
		metric.setMetricName("foo");
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_ACCUM_DIFF);
		metric.setColumn(0);
		metric.setMetricFile(file.getCanonicalPath());
		metric.setDecimalNumber(true);
		metric.initialize();
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(0, metric.getMetric().getValue().longValue());
		long diff = 10;
		long value2 = value1 + diff;
		writeToFile(file, value2 + "\n");
		metric.updateValue();
		assertEquals(diff, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAccumDiffMetricFloat() throws Exception {
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		File file = new File(tmpDir, "metricFile");
		FileMetric metric = new FileMetric();
		long value1 = 13413123123L;
		writeToFile(file, value1 + "\n");
		metric.setMetricName("foo");
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_ACCUM_DIFF);
		metric.setColumn(0);
		metric.setMetricFile(file.getCanonicalPath());
		metric.initialize();
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(0, metric.getMetric().getValue().longValue());
		long diff = 10;
		long value2 = value1 + diff;
		writeToFile(file, value2 + "\n");
		metric.updateValue();
		assertEquals(diff, metric.getMetric().getValue().longValue());
	}

	@Test(expected = IOException.class)
	public void testFileMetricMissingPrefix() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Unknown-prefix:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFileMetricNoKind() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setMetricName("foo");
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.initialize();
	}

	@Test(expected = IOException.class)
	public void testFileMetricEmptyFile() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/emptyFile");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test(expected = IOException.class)
	public void testInvalidColumn() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(100);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test(expected = IOException.class)
	public void testInvalidNumberFormat() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		// invalid column "kB"
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setPrefix("Cached:");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test
	public void testDirMetric() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.DIR);
		metric.setMetricFile(PROC_PREFIX + "/self/fd");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(4, metric.getMetric().getValue().intValue());
	}

	@Test
	public void testDirMetricAdjust() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.DIR);
		metric.setMetricFile(PROC_PREFIX + "/self/fd");
		metric.setAdjustmentOperation(FileMetricOperation.ADD);
		metric.setAdjustmentValue(1);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(5, metric.getMetric().getValue().intValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDirNotDir() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.DIR);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.initialize();
	}

	@Test
	public void testRegexPatternMetric() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(106596, metric.getMetric().getValue().longValue());
	}

	@Test(expected = IOException.class)
	public void testRegexPatternMetricWrongColumn() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		// wrong column number
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
	}

	@Test
	public void testAdjustmentAdd() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(1);
		metric.setAdjustmentOperation(FileMetricOperation.ADD);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(106597, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentSubtract() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(1);
		metric.setAdjustmentOperation(FileMetricOperation.SUBTRACT);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(106595, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentSubtractDecimal() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(1);
		metric.setAdjustmentOperation(FileMetricOperation.SUBTRACT);
		metric.setDecimalNumber(true);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(106595, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentMultiply() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(2);
		metric.setAdjustmentOperation(FileMetricOperation.MULTIPLY);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(213192, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentMultiplyDecimal() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(2);
		metric.setAdjustmentOperation(FileMetricOperation.MULTIPLY);
		metric.setDecimalNumber(true);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(213192, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentDivide() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(2);
		metric.setAdjustmentOperation(FileMetricOperation.DIVIDE);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(53298, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentDivideFloat() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(2.2);
		metric.setAdjustmentOperation(FileMetricOperation.DIVIDE);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(48452, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentDivideDecimal() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentValue(2);
		metric.setAdjustmentOperation(FileMetricOperation.DIVIDE);
		metric.setDecimalNumber(true);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(53298, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentDivideZero() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentOperation(FileMetricOperation.DIVIDE);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(0, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testAdjustmentDivideZeroLong() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLinePattern("Mapped:\\s+(\\d+).*");
		metric.setAdjustmentOperation(FileMetricOperation.DIVIDE);
		metric.setDecimalNumber(true);
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(0, metric.getMetric().getValue().longValue());
	}

	@Test
	public void testLineNumber() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.setColumn(1);
		metric.setLineNumber(17);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setLineSplit(" +");
		metric.initialize();
		assertNotNull(metric.getMetric());
		assertEquals(label, metric.getMetric().getName());
		assertTrue(metric.isInitialized());
		metric.updateValue();
		assertEquals(106596, metric.getMetric().getValue().longValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoFile() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setRequired(true);
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
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.initialize();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoColumnAccum() throws Exception {
		FileMetric metric = new FileMetric();
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		metric.setKind(FileMetricKind.FILE_ACCUM);
		metric.initialize();
	}

	@Test
	public void testIsNotEnabled() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/does-not-exist");
		metric.initialize();
		assertFalse(metric.isInitialized());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsNotEnabledThrow() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setDescription("desc");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(2);
		metric.setMetricFile(PROC_PREFIX + "/does-not-exist");
		metric.setRequired(true);
		metric.initialize();
		assertEquals(label, metric.getMetric().getName());
		assertFalse(metric.isInitialized());
	}

	@Test
	public void testModule() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		String component = "comp";
		metric.setMetricComponent(component);
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		String module = "mod";
		metric.setMetricModule(module);
		metric.initialize();
		assertEquals(component + "." + module + "." + label, metric.toString());
	}

	@Test
	public void testUnit() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFile(PROC_PREFIX + "/meminfo");
		String unit = "unit";
		metric.setUnit(unit);
		metric.initialize();
		assertEquals(unit, metric.getMetric().getUnit());
	}

	@Test
	public void testNoFiles() throws Exception {
		FileMetric metric = new FileMetric();
		String label = "foo";
		metric.setMetricName(label);
		metric.setMetricComponent("comp");
		metric.setKind(FileMetricKind.FILE_VALUE);
		metric.setColumn(1);
		metric.setMetricFiles(new String[0]);
		String unit = "unit";
		metric.setUnit(unit);
		metric.initialize();
	}

	private void writeToFile(File file, String contents) throws IOException {
		FileWriter writer = new FileWriter(file);
		try {
			writer.write(contents);
		} finally {
			writer.close();
		}
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
}
