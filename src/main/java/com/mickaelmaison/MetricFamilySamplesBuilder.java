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

import com.yammer.metrics.stats.Snapshot;
import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricFamilySamplesBuilder {

    private final Collector.Type type;
    private final String help;
    private final List<Collector.MetricFamilySamples.Sample> samples;

    public MetricFamilySamplesBuilder(Collector.Type type, String help) {
        this.type = type;
        this.help = help;
        this.samples = new ArrayList<>();
    }

    MetricFamilySamplesBuilder addSample(String name, double value, Map<String, String> labels) {
        samples.add(new Collector.MetricFamilySamples.Sample(
                        Collector.sanitizeMetricName(name),
                        new ArrayList<>(labels.keySet()),
                        new ArrayList<>(labels.values()),
                        value));
        return this;
    }

    MetricFamilySamplesBuilder addQuantileSamples(String name, Snapshot snapshot, Map<String, String> labels) {
        for (double quantile : new double[]{0.50, 0.75, 0.95, 0.98, 0.99, 0.999}) {
            Map<String, String> newLabels = new HashMap<>(labels);
            newLabels.put("quantile", String.valueOf(quantile));
            addSample(name, snapshot.getValue(quantile), newLabels);
        }
        return this;
    }

    Collector.MetricFamilySamples build() {
        if (samples.isEmpty()) {
            throw new IllegalStateException("There are no samples");
        }
        return new Collector.MetricFamilySamples(samples.get(0).name, type, help, samples);
    }
}
