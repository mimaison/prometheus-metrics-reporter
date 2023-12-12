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

import io.prometheus.client.Collector;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KafkaMetricsCollector extends Collector {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaMetricsCollector.class.getName());

    private final Map<MetricName, KafkaMetric> metrics;
    private final PrometheusMetricsReporterConfig config;
    private String prefix;

    public KafkaMetricsCollector(PrometheusMetricsReporterConfig config) {
        this.config = config;
        this.metrics = new ConcurrentHashMap<>();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>();

        for (Map.Entry<MetricName, KafkaMetric> entry : metrics.entrySet()) {
            MetricName metricName = entry.getKey();
            KafkaMetric kafkaMetric = entry.getValue();
            LOG.trace("Collecting Kafka metric {}", metricName);

            String name = metricName(metricName);
            // TODO Filtering should take labels into account
            if (!config.isAllowed(name)) {
                LOG.info("Kafka metric {} is not allowed", name);
                continue;
            }
            LOG.info("Kafka metric {} is allowed", name);
            LOG.info("labels " + metricName.tags());
            MetricFamilySamples sample = convert(name, metricName.description(), kafkaMetric, metricName.tags());
            if (sample != null) {
                samples.add(sample);
            }
        }
        return samples;
    }

    public void addMetric(KafkaMetric metric) {
        metrics.put(metric.metricName(), metric);
    }

    public void removeMetric(KafkaMetric metric) {
        metrics.remove(metric.metricName());
    }

    String metricName(MetricName metricName) {
        String prefix = this.prefix
                .replace('.', '_')
                .replace('-', '_')
                .toLowerCase();
        String group = metricName.group()
                .replace('.', '_')
                .replace('-', '_')
                .toLowerCase();
        String name = metricName.name()
                .replace('.', '_')
                .replace('-', '_')
                .toLowerCase();
        return prefix + '_' + group + '_' + name;
    }

    static MetricFamilySamples convert(String name, String help, KafkaMetric metric, Map<String, String> labels) {
        Object value = metric.metricValue();
        if (!(value instanceof Number)) {
            // Prometheus only accepts numeric metrics.
            // Kafka gauges can have arbitrary types, so skip them for now
            // TODO move non-numeric values to labels
            return null;
        }
        Map<String, String> sanitizedLabels = labels.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Collector.sanitizeMetricName(e.getKey()),
                        Map.Entry::getValue,
                        (v1, v2) -> { throw new IllegalStateException("Unexpected duplicate key " + v1); },
                        LinkedHashMap::new));
        return new MetricFamilySamplesBuilder(Type.GAUGE, help)
                .addSample(name, ((Number) value).doubleValue(), sanitizedLabels)
                .build();
    }
}
