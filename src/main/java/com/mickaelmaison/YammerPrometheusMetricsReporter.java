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
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YammerPrometheusMetricsReporter implements KafkaMetricsReporter {

    private static final Logger LOG = LoggerFactory.getLogger(YammerPrometheusMetricsReporter.class.getName());

    @Override
    public void init(VerifiableProperties props) {
        LOG.info(">>> in init() yammer");
        PrometheusMetricsReporterConfig config = new PrometheusMetricsReporterConfig(props.props());
        LOG.info("yammer defaultRegistry" + CollectorRegistry.defaultRegistry);
        CollectorRegistry.defaultRegistry.register(new YammerMetricsCollector(config));
    }

}
