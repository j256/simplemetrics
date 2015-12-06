package com.j256.simplemetrics.publisher;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.j256.simplemetrics.manager.MetricsManager;
import com.j256.simplemetrics.manager.MetricsUpdater;
import com.j256.simplemetrics.metric.ControlledMetricValue;

/**
 * Class which outputs some important system metrics internal to the JVM.
 * 
 * @author graywatson
 */
public class SystemMetricsPublisher implements MetricsUpdater {

	private static final String METRIC_COMPONENT_NAME = "java";
	private ClassLoadingMXBean classLoadingMxBean;
	private MemoryPoolMXBean oldGenMxBean;
	private ThreadMXBean threadMxBean;
	private long lastCpuPollTimeMillis;
	private long lastCpuTimeMillis;
	private final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
	private static final ObjectName operatingSystemObject;

	private MetricsManager metricsManager;

	private ControlledMetricValue numberThreads;
	private ControlledMetricValue totalMemory;
	private ControlledMetricValue maxMemory;
	private ControlledMetricValue freeMemory;
	private ControlledMetricValue currentHeap;
	private ControlledMetricValue loadedClasses;
	private ControlledMetricValue totalCpuTime;
	private ControlledMetricValue threadLoadAveragePercentage;
	private ControlledMetricValue oldGenMemoryPercentageUsed;
	private ControlledMetricValue processLoadAveragePercentage;

	static {
		try {
			operatingSystemObject = ObjectName.getInstance("java.lang:type=OperatingSystem");
		} catch (MalformedObjectNameException mone) {
			throw new RuntimeException(mone);
		}
	}

	public SystemMetricsPublisher() {
		// for spring
	}

	public SystemMetricsPublisher(MetricsManager metricsManager) {
		this.metricsManager = metricsManager;
		initialize();
	}

	/**
	 * Should be called after all of the setter methods have been completed. Maybe by Spring's init mechanism?
	 */
	public void initialize() {
		classLoadingMxBean = ManagementFactory.getClassLoadingMXBean();
		// get the mbeans associated with the GC pools
		List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
		for (MemoryPoolMXBean bean : beans) {
			// we are interested in the "old gen" (java 6) or "tenured gen" (java 5) pool
			if (bean.getName().equalsIgnoreCase("PS Old Gen") || bean.getName().equalsIgnoreCase("Tenured Gen")) {
				oldGenMxBean = bean;
				break;
			}
		}
		threadMxBean = ManagementFactory.getThreadMXBean();

		// create our metrics
		numberThreads =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "threads", "numThreads", "Number of running threads",
						"count");
		totalMemory =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "mem", "totalMemory",
						"Total memory in use by the jvm", "bytes");
		maxMemory =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "mem", "maxMemory",
						"Max memory the jvm will attempt to use", "bytes");
		freeMemory =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "mem", "freeMemory",
						"Amount of free memory in the jvm", "bytes");
		currentHeap =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "mem", "currentHeap",
						"Amount of memory currently used by the jvm", "bytes");
		loadedClasses =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "mem", "loadedClasses",
						"Number of classes loaded by the jvm", "count");
		totalCpuTime =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "cpu", "totalCpuTime",
						"Total cpu time from the threads in milliseconds", "milliseconds");
		threadLoadAveragePercentage =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "cpu", "threadLoadAvgPerc",
						"Load average percentage from the thread list", "percent");
		oldGenMemoryPercentageUsed =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "mem", "oldGenMemUsedPerc",
						"Old Gen GC pool percentage of used memory", "percent");
		processLoadAveragePercentage =
				new ControlledMetricValue(METRIC_COMPONENT_NAME, "cpu", "processCpuTime",
						"Process cpu time percentage", "percentage");

		metricsManager.registerMetric(numberThreads);
		metricsManager.registerMetric(totalMemory);
		metricsManager.registerMetric(maxMemory);
		metricsManager.registerMetric(freeMemory);
		metricsManager.registerMetric(currentHeap);
		metricsManager.registerMetric(loadedClasses);
		metricsManager.registerMetric(totalCpuTime);
		metricsManager.registerMetric(threadLoadAveragePercentage);
		metricsManager.registerMetric(oldGenMemoryPercentageUsed);
		metricsManager.registerMetric(processLoadAveragePercentage);
		metricsManager.registerUpdatePoll(this);
	}

	/**
	 * See {@link MetricsUpdater#updateMetrics()}
	 */
	@Override
	public void updateMetrics() {
		numberThreads.adjustValue(Thread.activeCount());
		Runtime runtime = Runtime.getRuntime();
		totalMemory.adjustValue(runtime.totalMemory());
		maxMemory.adjustValue(runtime.maxMemory());
		freeMemory.adjustValue(runtime.freeMemory());
		currentHeap.adjustValue(runtime.totalMemory() - runtime.freeMemory());
		if (classLoadingMxBean != null) {
			loadedClasses.adjustValue(classLoadingMxBean.getLoadedClassCount());
		}
		totalCpuTime.adjustValue(getTotalCpuTimeMillis());
		threadLoadAveragePercentage.adjustValue((long) (calcLoadAveragePercentage() * 100));
		if (oldGenMxBean != null) {
			// calculate our memory usage
			MemoryUsage usage = oldGenMxBean.getUsage();
			oldGenMemoryPercentageUsed.adjustValue(usage.getUsed() * 100L / usage.getMax());
		}
		processLoadAveragePercentage.adjustValue(extractProcessLoadAveragePercentage());
	}

	// @Required
	public void setMetricsManager(MetricsManager metricsManager) {
		this.metricsManager = metricsManager;
	}

	public long getTotalCpuTimeMillis() {
		long total = 0;
		for (long id : threadMxBean.getAllThreadIds()) {
			long cpuTime = threadMxBean.getThreadCpuTime(id);
			if (cpuTime > 0) {
				total += cpuTime;
			}
		}
		// since is in nano-seconds
		long currentCpuMillis = total / 1000000;
		return currentCpuMillis;
	}

	private double calcLoadAveragePercentage() {
		long now = System.currentTimeMillis();
		if (lastCpuPollTimeMillis == 0) {
			lastCpuPollTimeMillis = now;
		}
		long timeDiff = now - lastCpuPollTimeMillis;
		if (timeDiff == 0) {
			timeDiff = 1;
		}
		long currentCpuTimeMillis = getTotalCpuTimeMillis();
		if (lastCpuPollTimeMillis == 0) {
			lastCpuTimeMillis = currentCpuTimeMillis;
		}
		long cpuDiff = currentCpuTimeMillis - lastCpuTimeMillis;
		double loadAvg = (double) cpuDiff / (double) timeDiff;
		return loadAvg;
	}

	private double extractProcessLoadAveragePercentage() {
		AttributeList list;
		try {
			list = mbeanServer.getAttributes(operatingSystemObject, new String[] { "ProcessCpuLoad" });
		} catch (Exception e) {
			// ignore it
			return 0.0;
		}
		if (list.isEmpty()) {
			return 0.0;
		}

		Attribute att = (Attribute) list.get(0);
		Double value = (Double) att.getValue();

		// usually takes a couple of seconds before we get real values
		if (value < 0.0) {
			return 0.0;
		} else {
			// returns a percentage value;
			return value * 100.0;
		}
	}
}
