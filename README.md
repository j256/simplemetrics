Simple Java Metrics
===================

This package provides some Java classes to help with the providing application metrics.

* For more information, visit the [SimpleMetrics home page](http://256stuff.com/sources/simplemetrics/).	
* Online documentation can be found off the home page.  Here are the [code Javadocs](http://256stuff.com/sources/simplemetrics/javadoc/simplemetrics/).
* Browse the code on the [git repository](https://github.com/j256/simplemetrics).  [![CircleCI](https://circleci.com/gh/j256/simplemetrics.svg?style=svg)](https://circleci.com/gh/j256/simplemetrics) [![CodeCov](https://img.shields.io/codecov/c/github/j256/simplemetrics.svg)](https://codecov.io/github/j256/simplemetrics/)
* Maven packages are published via [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.j256.simplemetrics/simplemetrics/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.j256.simplemetrics/simplemetrics/) [![javadoc](https://javadoc.io/badge2/com.j256.simplemetrics/simplemetrics/javadoc.svg)](https://javadoc.io/doc/com.j256.simplemetrics/simplemetrics)

Enjoy, Gray Watson

## Little Sample Program

I've checked in a [little example program](http://256stuff.com/sources/simplemetrics/docs/example-simple).

## Getting Started

Here's a quick code sample showing how to get started.

	// create our metrics manager which controls everything
	MetricsManager metricsManager = new MetricsManager();

	// create and register a persister, you will probably want to write your own for your own logging
	LoggingMetricsPersister persister = new LoggingMetricsPersister();
	metricsManager.setMetricValuesPersisters(new MetricValuesPersister[] { persister });

	// create and register one (or many) metrics
	ControlledMetricAccum hitCounter =
		new ControlledMetricAccum("example", null, "hits", "number of hits to the cache", null);
	metricsManager.registerMetric(hitCounter);

	// optionally start a persister thread to persist the metrics every minute (60000 millis)
	MetricsPersisterJob persisterThread = new MetricsPersisterJob(manager, 60000, 60000, true);

	// now the metric can be incremented whenever a "hit" occurs
	hitCounter.increment();
	// or maybe we need to account for a bunch of hits
	hitCounter.add(23);
	// ...
	
	// the persister will log the value of hitCounter every 60 seconds automatically

# Maven Configuration

``` xml
<dependencies>
	<dependency>
		<groupId>com.j256.simplemetrics</groupId>
		<artifactId>simplemetrics</artifactId>
		<!-- NOTE: change the version to the most recent release version from the repo -->
		<version>1.10</version>
	</dependency>
</dependencies>
```

# ChangeLog Release Notes

See the [ChangeLog.txt file](src/main/javadoc/doc-files/changelog.txt).
