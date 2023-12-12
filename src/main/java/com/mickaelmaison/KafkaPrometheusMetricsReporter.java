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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsContext;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class KafkaPrometheusMetricsReporter implements MetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaPrometheusMetricsReporter.class.getName());

    private KafkaMetricsCollector kafkaMetricsCollector;
    private Optional<HTTPServer> httpServer;

    @Override
    public void configure(Map<String, ?> map) {
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(map);
        kafkaMetricsCollector = new KafkaMetricsCollector(config);
        // Add JVM metrics
        DefaultExports.initialize();
        httpServer = config.startHttpServer();
    }

    @Override
    public void init(List<KafkaMetric> metrics) {
        CollectorRegistry.defaultRegistry.register(kafkaMetricsCollector);
        for (KafkaMetric metric : metrics) {
            metricChange(metric);
        }
    }

    @Override
    public void metricChange(KafkaMetric metric) {
        LOG.info("Kafka metricChange " + metric.metricName());
        kafkaMetricsCollector.addMetric(metric);
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        LOG.info("Kafka metricRemoval " + metric.metricName());
        kafkaMetricsCollector.removeMetric(metric);
    }

    @Override
    public void close() {
        LOG.info("Closing the HTTP server");
    }

    @Override
    public void reconfigure(Map<String, ?> configs) {
    }

    @Override
    public void validateReconfiguration(Map<String, ?> configs) throws ConfigException {
    }

    @Override
    public Set<String> reconfigurableConfigs() {
        return Collections.emptySet();
    }

    @Override
    public void contextChange(MetricsContext metricsContext) {
        LOG.info("Kafka contextChange with " + metricsContext.contextLabels());
        String prefix = metricsContext.contextLabels().get(MetricsContext.NAMESPACE);
        kafkaMetricsCollector.setPrefix(prefix);
    }

    public int getPort() {
        return httpServer.get().getPort();
    }
}
