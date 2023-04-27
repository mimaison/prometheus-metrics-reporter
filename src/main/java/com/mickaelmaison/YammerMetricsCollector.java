/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mickaelmaison;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import io.prometheus.client.Collector;
import org.apache.kafka.server.metrics.KafkaYammerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YammerMetricsCollector extends Collector {

    private static final Logger LOG = LoggerFactory.getLogger(YammerMetricsCollector.class.getName());

    private final MetricsRegistry registry;
    private final PrometheusMetricsReporterConfig config;

    public YammerMetricsCollector(PrometheusMetricsReporterConfig config) {
        this.config = config;
        this.registry = KafkaYammerMetrics.defaultRegistry();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>();

        for (Map.Entry<MetricName, Metric> entry : registry.allMetrics().entrySet()) {
            MetricName metricName = entry.getKey();
            Metric metric = entry.getValue();
            LOG.trace("Collecting Yammer metric {}", metricName);

            String name = metricName(metricName);
            if (!config.isAllowed(name)) {
                continue;
            }
            Map<String, String> labels = labelsFromScope(metricName.getScope());

            MetricFamilySamples sample = null;
            if (metric instanceof Counter) {
                sample = convert(name, (Counter) metric, labels);
            } else if (metric instanceof Gauge) {
                sample = convert(name, (Gauge<?>) metric, labels);
            } else if (metric instanceof Histogram) {
                sample = convert(name, (Histogram) metric, labels);
            } else if (metric instanceof Meter) {
                sample = convert(name, (Meter) metric, labels);
            } else if (metric instanceof Timer) {
                sample = convert(name, (Timer) metric, labels);
            } else {
                LOG.error("The metric " + metric.getClass().getName() + " has an unexpected type.");
            }
            if (sample != null) {
                samples.add(sample);
            }
        }
        return samples;
    }

    static String metricName(MetricName metricName) {
        String name = metricName.getGroup() + "_" + metricName.getType() + "_" + metricName.getName();
        return name.toLowerCase();
    }

    static Map<String, String> labelsFromScope(String scope) {
        if (scope != null) {
            String[] parts = scope.split("\\.");
            if (parts.length % 2 == 0) {
                Map<String, String> labels = new HashMap<>();
                for (int i = 0; i < parts.length; i += 2) {
                    labels.put(parts[i], parts[i + 1]);
                }
                return labels;
            }
        }
        return Collections.emptyMap();
    }

    static MetricFamilySamples convert(String name, Counter counter, Map<String, String> labels) {
        return new MetricFamilySamplesBuilder(Type.GAUGE, "")
                .addSample(name, counter.count(), labels)
                .build();
    }

    static MetricFamilySamples convert(String name, Gauge<?> gauge, Map<String, String> labels) {
        Object value = gauge.value();
        if (!(value instanceof Number)) {
            // Prometheus only accepts numeric metrics.
            // Some Kafka gauges have string values (for example kafka.server:type=KafkaServer,name=ClusterId), so skip them
            return null;
        }
        return new MetricFamilySamplesBuilder(Type.GAUGE, "")
                .addSample(name, ((Number) value).doubleValue(), labels)
                .build();
    }

    static MetricFamilySamples convert(String name, Meter meter, Map<String, String> labels) {
        return new MetricFamilySamplesBuilder(Type.COUNTER, "")
                .addSample(name, meter.count(), labels)
                .build();
    }

    static MetricFamilySamples convert(String name, Histogram histogram, Map<String, String> labels) {
        return new MetricFamilySamplesBuilder(Type.SUMMARY, "")
                .addSample(name, histogram.count(), labels)
                .addQuantileSamples(name, histogram.getSnapshot(), labels)
                .build();
    }

    static MetricFamilySamples convert(String name, Timer metric, Map<String, String> labels) {
        return new MetricFamilySamplesBuilder(Type.SUMMARY, "")
                .addSample(name, metric.count(), labels)
                .addQuantileSamples(name, metric.getSnapshot(), labels)
                .build();
    }
}
