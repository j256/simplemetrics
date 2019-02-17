package com.j256.simplemetrics.persister;

import java.io.File;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;

/**
 * JMX information for the text-file metrics publisher.
 * 
 * @author graywatson
 */
@JmxResource(beanName = "TextFilePersister", description = "Text File Metrics Persister")
public class TextFileMetricsPersisterJmx implements JmxSelfNaming {

	private TextFileMetricsPersister metricsPersister;
	private String jmxDomainName = "com.j256";
	private JmxFolderName[] jmxFolderNames = { new JmxFolderName("metrics") };

	public TextFileMetricsPersisterJmx() {
		// for spring
	}

	public TextFileMetricsPersisterJmx(TextFileMetricsPersister metricsPersister) {
		this.metricsPersister = metricsPersister;
	}

	@JmxAttributeMethod(description = "File prefix we are writing")
	public String getLogFileNamePrefix() {
		return metricsPersister.getLogFileNamePrefix();
	}

	@JmxAttributeMethod(description = "Whether we are appending the sys time millis to the output file")
	public boolean isAppendSysTimeMillis() {
		return metricsPersister.isAppendSysTimeMillis();
	}

	@JmxAttributeMethod(description = "Whether we are appending the sys time millis to the output file")
	public void setAppendSysTimeMillis(boolean appendSysTimeMillis) {
		metricsPersister.setAppendSysTimeMillis(appendSysTimeMillis);
	}

	@JmxAttributeMethod(description = "Directory where log files are written")
	public File getOutputDirectory() {
		return metricsPersister.getOutputDirectory();
	}

	@JmxAttributeMethod(description = "Show the description in the file")
	public boolean isShowDescription() {
		return metricsPersister.isShowDescription();
	}

	@JmxAttributeMethod(description = "Show the description in the file")
	public void setShowDescription(boolean showDescription) {
		metricsPersister.setShowDescription(showDescription);
	}

	@JmxAttributeMethod(description = "Number of times we've written metrics")
	public long getDumpLogCount() {
		return metricsPersister.getDumpLogCount();
	}

	@JmxAttributeMethod(description = "Number of times we've deleted metrics files")
	public long getCleanupLogCount() {
		return metricsPersister.getCleanupLogCount();
	}

	@JmxAttributeMethod(description = "Last time the metrics were written")
	public String getLastDumpTimeMillisString() {
		return metricsPersister.getLastDumpTimeMillisString();
	}

	// @Required
	public void setMetricsPersister(TextFileMetricsPersister metricsPersister) {
		this.metricsPersister = metricsPersister;
	}

	@Override
	public String getJmxBeanName() {
		// use the defined one above
		return null;
	}

	@Override
	public String getJmxDomainName() {
		return jmxDomainName;
	}

	/**
	 * Set the domain-name top-level folder for the metrics. Default is "com.j256".
	 */
	// @NotRequired("default is com.j256")
	public void setJmxDomainName(String jmxDomainName) {
		this.jmxDomainName = jmxDomainName;
	}

	@Override
	public JmxFolderName[] getJmxFolderNames() {
		return jmxFolderNames;
	}

	/**
	 * Set the sub-folder name for the metrics. Default is {"metrics"}.
	 */
	// @NotRequired("default is { \"metrics\" }")
	public void setJmxFolderNames(String[] folderNames) {
		JmxFolderName[] jmxFolderNames = new JmxFolderName[folderNames.length];
		for (int i = 0; i < jmxFolderNames.length; i++) {
			jmxFolderNames[i] = new JmxFolderName(folderNames[i]);
		}
		this.jmxFolderNames = jmxFolderNames;
	}
}
