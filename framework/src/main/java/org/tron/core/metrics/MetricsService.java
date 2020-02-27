package org.tron.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import java.util.SortedMap;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.metrics.blockchain.BlockChainMetricManager;

@Slf4j(topic = "metrics")
@Component
public class MetricsService {

  @Setter
  private BlockChainMetricManager blockChainMetricManager;

  private MetricRegistry metricRegistry = new MetricRegistry();

  @Getter
  private long failProcessBlockNum = 0;

  @Getter
  private String failProcessBlockReason = "";

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
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.histogram(key).update(value);
      }
    } catch (Exception e) {
      logger.warn("update histogram failed, key:{}, value:{}", key, value);
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
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.meter(key).mark(value);
      }
    } catch (Exception e) {
      logger.warn("mark meter failed, key:{}, value:{}", key, value);
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
    try {
      if (CommonParameter.getInstance().isNodeMetricsEnable()) {
        metricRegistry.counter(key).inc(value);
      }
    } catch (Exception e) {
      logger.warn("inc counter failed, key:{}, value:{}", key, value);
    }
  }

  /**
   * apply block.
   *
   * @param block BlockCapsule
   */
  public void applyBlock(BlockCapsule block) {
    try {
      blockChainMetricManager.applyBlock(block);
    } catch (Exception e) {
      logger.warn("record block failed, {}, reason: {}.",
              block.getBlockId().toString(), e.getMessage());
    }
  }

  public void failProcessBlock(long blockNum, String errorInfo) {
    failProcessBlockNum = blockNum;
    failProcessBlockReason = errorInfo;
  }

  public MetricsInfo getMetricsInfo() {
    return new MetricsInfo();
  }

}
