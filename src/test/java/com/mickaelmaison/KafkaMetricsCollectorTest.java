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
import org.apache.kafka.common.metrics.Gauge;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.utils.Time;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KafkaMetricsCollectorTest {

    private final MetricConfig metricConfig = new MetricConfig();
    private final Time time = Time.SYSTEM;
    private Map<String, String> labels;

    @Before
    public void setup() {
        labels = new HashMap<>();
        labels.put("key", "value");
    }

    @Test
    public void testCollect() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server_group_name.*");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);
        KafkaMetricsCollector collector = new KafkaMetricsCollector(config);
        collector.setPrefix("kafka.server");

        List<Collector.MetricFamilySamples> metrics = collector.collect();
        assertTrue(metrics.isEmpty());

        // Adding a metric not matching the allowlist does nothing
        collector.addMetric(buildMetric("name", "other", 2.0));
        metrics = collector.collect();
        assertTrue(metrics.isEmpty());

        // Adding a non-numeric metric does nothing
        collector.addMetric(buildNonNumericMetric("name2", "group"));
        metrics = collector.collect();
        assertTrue(metrics.isEmpty());

        // Adding a metric that matches the allowlist
        collector.addMetric(buildMetric("name", "group", 1.0));
        metrics = collector.collect();
        assertEquals(1, metrics.size());
        assertEquals("kafka_server_group_name", metrics.get(0).name);
        assertEquals(1, metrics.get(0).samples.size());
        assertEquals(1.0, metrics.get(0).samples.get(0).value, 0.1);
        assertEquals(new ArrayList<>(labels.keySet()), metrics.get(0).samples.get(0).labelNames);
        assertEquals(new ArrayList<>(labels.values()), metrics.get(0).samples.get(0).labelValues);

        // Adding the same metric updates its value
        collector.addMetric(buildMetric("name", "group", 3.0));
        metrics = collector.collect();
        assertEquals(1, metrics.size());
        assertEquals("kafka_server_group_name", metrics.get(0).name);
        assertEquals(1, metrics.get(0).samples.size());
        assertEquals(3.0, metrics.get(0).samples.get(0).value, 0.1);

        // Removing the metric
        collector.removeMetric(buildMetric("name", "group", 4.0));
        metrics = collector.collect();
        assertTrue(metrics.isEmpty());
    }

    private KafkaMetric buildMetric(String name, String group, double value) {
        Measurable measurable = (config, now) -> value;
        return new KafkaMetric(
                new Object(),
                new MetricName(name, group, "", labels),
                measurable,
                metricConfig,
                time);
    }

    private KafkaMetric buildNonNumericMetric(String name, String group) {
        Gauge<String> measurable = (config, now) -> "hello";
        return new KafkaMetric(
                new Object(),
                new MetricName(name, group, "", labels),
                measurable,
                metricConfig,
                time);
    }

}
