# prometheus-metrics-reporter

This allows exposing metrics from Kafka brokers and client to Prometheus. 

This uses the metrics reporter interfaces of Kafka to retrieve metrics instead of using JMX.

## Build

```shell
mvn package assembly:single
```

## Run

### Kafka Brokers
Add the following to your broker configuration:
```properties
metric.reporters=com.mickaelmaison.KafkaPrometheusMetricsReporter
kafka.metrics.reporters=com.mickaelmaison.YammerPrometheusMetricsReporter
```

### Kafka Clients
Add the following to your client configuration:
```properties
metric.reporters=com.mickaelmaison.KafkaPrometheusMetricsReporter
```

## Access Metrics

Metrics are exposed on [http://localhost:8080/metrics](http://localhost:8080/metrics)
