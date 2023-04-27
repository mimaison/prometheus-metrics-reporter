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

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PrometheusMetricsReporterConfigTest {

    @Test
    public void testDefaults() {
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(Collections.emptyMap());

        assertEquals(PrometheusMetricsReporterConfig.PORT_CONFIG_DEFAULT, config.port());
        assertTrue(config.isAllowed("random_name"));
    }

    @Test
    public void testOverrides() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.PORT_CONFIG, "1234");
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server.*");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        assertEquals(1234, config.port());
        assertFalse(config.isAllowed("random_name"));
        assertTrue(config.isAllowed("kafka_server_metric"));
    }

    @Test
    public void testAllowlist() {
        Map<String, String> props = new HashMap<>();
        props.put(PrometheusMetricsReporterConfig.ALLOWLIST_CONFIG, "kafka_server.*,kafka_network.*");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props);

        assertFalse(config.isAllowed("random_name"));
        assertTrue(config.isAllowed("kafka_server_metric"));
        assertTrue(config.isAllowed("kafka_network_metric"));
    }
}
