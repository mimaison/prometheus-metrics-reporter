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

import io.prometheus.client.exporter.HTTPServer;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class PrometheusMetricsReporterConfig extends AbstractConfig {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetricsReporterConfig.class.getName());

    public static final String CONFIG_PREFIX = "prometheus.metrics.reporter.";

    public static final String PORT_CONFIG = CONFIG_PREFIX + "port";
    public static final int PORT_CONFIG_DEFAULT = 8080;
    public static final String PORT_CONFIG_DOC = "The HTTP port to expose the metrics.";

    public static final String ALLOWLIST_CONFIG = CONFIG_PREFIX + "allowlist";
    public static final String ALLOWLIST_CONFIG_DEFAULT = ".*";
    public static final String ALLOWLIST_CONFIG_DOC = "A comma separated list of regex Patterns to specify the metrics to collect.";

    private static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(PORT_CONFIG, ConfigDef.Type.INT, PORT_CONFIG_DEFAULT, ConfigDef.Importance.HIGH, PORT_CONFIG_DOC)
            .define(ALLOWLIST_CONFIG, ConfigDef.Type.LIST, ALLOWLIST_CONFIG_DEFAULT, ConfigDef.Importance.HIGH, ALLOWLIST_CONFIG_DOC);

    private final int port;
    private final Pattern allowlist;

    public PrometheusMetricsReporterConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        this.port = getInt(PORT_CONFIG);
        this.allowlist = compileAllowlist(getList(ALLOWLIST_CONFIG));
    }

    public int port() {
        return port;
    }

    public boolean isAllowed(String name) {
        return allowlist.matcher(name).matches();
    }

    private Pattern compileAllowlist(List<String> allowlist) {
        String joined = String.join("|", allowlist);
        return Pattern.compile(joined);
    }

    @Override
    public String toString() {
        return "PrometheusMetricsReporterConfig{" +
                "allowlist=" + allowlist +
                ", port=" + port +
                '}';
    }

    public synchronized Optional<HTTPServer> startHttpServer() {
        // TODO if port is -1, don't start the server
        // TODO the HTTPServer instance is never closed
        try {
            HTTPServer httpServer = new HTTPServer(port, true);
            LOG.info("HTTP server started on port " + port);
            new Exception().printStackTrace();
            return Optional.of(httpServer);
        } catch (BindException be) {
            LOG.info("HTTP server already started");
            return Optional.empty();
        } catch (IOException ioe) {
            LOG.error("Failed starting HTTP server", ioe);
            throw new RuntimeException(ioe);
        }
    }

}
