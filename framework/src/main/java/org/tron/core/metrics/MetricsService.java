package org.tron.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.SortedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.protos.Protocol.Block;

@Component
public class MetricsService {

  private MetricRegistry metricRegistry = new MetricRegistry();

  private static MetricsService metricsService;
  public void init() {
    metricsService = this;
  }
  public static MetricsService getInstance() {
    return metricsService;
  }


  public Histogram getHistogram(String key) {
    return metricRegistry.histogram(key);
  }

  public SortedMap<String, Histogram> getHistograms(String key) {
    return metricRegistry.getHistograms((s, metric) -> s.startsWith(key));
  }

  /**
   * Histogram update.
   * @param key String
   * @param value long
   */
  public void histogramUpdate(String key, long value) {
    if (CommonParameter.getInstance().isNodeMetricsEnable()) {
      metricRegistry.histogram(key).update(value);
    }

  }

  public Meter getMeter(String name) {
    return metricRegistry.meter(name);
  }

  /**
   * Meter mark.
   * @param key String
   * @param value long
   */
  public void meterMark(String key, long value) {
    if (CommonParameter.getInstance().isNodeMetricsEnable()) {
      metricRegistry.meter(key).mark(value);
    }
  }

  public Counter getCounter(String name) {
    return metricRegistry.counter(name);
  }

  public SortedMap<String, Counter> getCounters(String name) {
    return metricRegistry.getCounters((s, metric) -> s.startsWith(name));
  }

  /**
   * Counter inc.
   * @param key String
   * @param value long
   */
  public void counterInc(String key, long value) {
    if (CommonParameter.getInstance().isNodeMetricsEnable()) {
      metricRegistry.counter(key).inc(value);
    }
  }

  public void applyBlcok(Block block) {
    // witness version, lantency,
  }

  public void failProcessBlcok(Block block, String errorInfo) {
    // witness version, lantency,
  }

  public MetricsInfo getMetricsInfo() {
    return new MetricsInfo();
  }

}
