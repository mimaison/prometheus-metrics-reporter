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
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrometheusMetricsReporter implements KafkaMetricsReporter, MetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetricsReporter.class.getName());
    private static final int PORT = 8080;

    private HTTPServer httpServer;
    private KafkaMetricsCollector kafkaMetricsCollector;

    @Override
    public void configure(Map<String, ?> map) {
        kafkaMetricsCollector = new KafkaMetricsCollector();
        try {
            httpServer = new HTTPServer(PORT, true);
        } catch (IOException ioe) {
            LOG.error("Failed starting HTTP server", ioe);
            throw new RuntimeException(ioe);
        }
        // Add JVM metrics
        DefaultExports.initialize();
    }

    @Override
    public void init(VerifiableProperties props) {
        CollectorRegistry.defaultRegistry.register(new YammerMetricsCollector());
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
        kafkaMetricsCollector.addMetric(metric);
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        kafkaMetricsCollector.removeMetric(metric);
    }

    @Override
    public void close() {
        if (httpServer != null) {
            httpServer.close();
        }
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
}
