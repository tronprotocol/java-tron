package org.tron.core.services.monitor;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MonitorMetric {

  public static final String NET_BLOCK_LATENCY = "net.block.latency";
  public static final String BLOCKCHAIN_TPS = "blockchain.TPS";

  @Autowired
  private MetricRegistry metricRegistry;

  public Histogram getHistogram(String name) {
    return metricRegistry.histogram(name);
  }

  public Meter getMeter(String name) {
    return metricRegistry.meter(name);
  }
}
