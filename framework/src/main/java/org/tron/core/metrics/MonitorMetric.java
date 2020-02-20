package org.tron.core.metrics;

import com.codahale.metrics.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.SortedMap;

@Component
public class MonitorMetric {

  public static final String NET_BLOCK_LATENCY = "net.block.latency";
  public static final String BLOCKCHAIN_TPS = "blockchain.TPS";
  public static final String NET_DISCONNECTION_COUNT = "net.disconnection.count";
  public static final String NET_DISCONNECTION_REASON = "net.disconnection.reason";

  @Autowired
  private MetricRegistry metricRegistry;

  public Histogram getHistogram(String name) {
    return metricRegistry.histogram(name);
  }

  public Meter getMeter(String name) {
    return metricRegistry.meter(name);
  }

  public Counter getCounter(String name) {
    return metricRegistry.counter(name);
  }

  public SortedMap<String, Counter> getCounters(String name) {
    return metricRegistry.getCounters((s, metric) -> s.startsWith(name));
  }
}
