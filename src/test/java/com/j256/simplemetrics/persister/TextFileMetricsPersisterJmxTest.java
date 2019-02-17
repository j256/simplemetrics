package com.j256.simplemetrics.persister;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.j256.simplemetrics.metric.ControlledMetric;
import com.j256.simplemetrics.metric.ControlledMetricValue;

public class TextFileMetricsPersisterJmxTest {

	private final String TEMP_DIR = "target/" + getClass().getSimpleName();

	@Test
	public void testBasic() throws IOException {
		TextFileMetricsPersister persister = new TextFileMetricsPersister();
		String prefix = "log.";
		persister.setLogFileNamePrefix(prefix);
		File tmpDir = new File(TEMP_DIR);
		tmpDir.mkdirs();
		persister.setOutputDirectory(tmpDir);

		TextFileMetricsPersisterJmx jmx = new TextFileMetricsPersisterJmx();
		jmx.setMetricsPersister(persister);

		String label = "4233h32oh343242";
		String component = "comp";
		String model = "mod";
		ControlledMetricValue metric = new ControlledMetricValue(component, model, label, "desc", "unit");
		assertEquals(0, jmx.getDumpLogCount());
		long dumpTime = System.currentTimeMillis();
		persister.persist(metricValueMap(metric), dumpTime);
		assertEquals(1, jmx.getDumpLogCount());
		assertEquals(tmpDir, jmx.getOutputDirectory());
		assertEquals(0, jmx.getCleanupLogCount());
		assertEquals(persister.getLastDumpTimeMillisString(), jmx.getLastDumpTimeMillisString());
	}

	private Map<ControlledMetric<?, ?>, Number> metricValueMap(ControlledMetric<?, ?>... metrics) {
		Map<ControlledMetric<?, ?>, Number> metricMap = new HashMap<ControlledMetric<?, ?>, Number>();
		for (ControlledMetric<?, ?> metric : metrics) {
			metricMap.put(metric, metric.getValueToPersist());
		}
		return metricMap;
	}
}
