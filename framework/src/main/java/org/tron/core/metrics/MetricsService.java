package org.tron.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.capsule.BlockCapsule;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;

@Slf4j(topic = "metrics")
@Component
public class MetricsService {

  private MetricRegistry metricRegistry = new MetricRegistry();

  private Map<String, BlockHeader> witnessInfo = new ConcurrentHashMap<String, BlockHeader>();

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

  public void applyBlock(BlockCapsule block, long nowTime) {
    try {
      String witnessAddress = Hex.toHexString(block.getWitnessAddress().toByteArray());

      //witness info
      if (witnessInfo.containsKey(witnessAddress)) {
        BlockHeader old = witnessInfo.get(witnessAddress);
        if (old.getRawData().getNumber() == block.getNum() &&
                Math.abs(old.getRawData().getTimestamp() - block.getTimeStamp()) < 3000) {
          counterInc(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT + witnessAddress, 1);
        }
      }
      witnessInfo.put(witnessAddress, block.getInstance().getBlockHeader());

      //latency
      long netTime = nowTime - block.getTimeStamp();
      histogramUpdate(MetricsKey.NET_BLOCK_LATENCY, netTime);
      histogramUpdate(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress, netTime);
      if (netTime >= 1000) {
        counterInc(MetricsKey.NET_BLOCK_LATENCY + ".1S", 1L);
        counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".1S", 1L);
        if (netTime >= 2000) {
          counterInc(MetricsKey.NET_BLOCK_LATENCY + ".2S", 1L);
          counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".2S", 1L);
          if (netTime >= 3000) {
            counterInc(MetricsKey.NET_BLOCK_LATENCY + ".3S", 1L);
            counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".3S", 1L);
          }
        }
      }

      //TPS
      if (block.getTransactions().size() > 0) {
        meterMark(MetricsKey.BLOCKCHAIN_TPS, block.getTransactions().size());
      }
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
