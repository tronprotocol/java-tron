package org.tron.core.metrics;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.metrics.blockchain.BlockChainMetricManager;

@Slf4j(topic = "metrics")
@Component
public class MetricsService {

  @Autowired
  private BlockChainMetricManager blockChainMetricManager;

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

  /**
   * fail process block.
   * @param blockNum long
   * @param errorInfo String
   */
  public void failProcessBlock(long blockNum, String errorInfo) {
    try {
      blockChainMetricManager.setFailProcessBlockNum(blockNum);
      blockChainMetricManager.setFailProcessBlockReason(errorInfo);
    } catch (Exception e) {
      logger.warn("record fail process block failed, {}, reason: {}.",
              blockNum, errorInfo);
    }
  }

  /**
   * collect block latency.
   * @param block BlockCapsule
   */
  public void collectLatencyInfo(BlockCapsule block) {
    try {
      long netTime = System.currentTimeMillis() - block.getTimeStamp();
      String witnessAddress = Hex.toHexString(block.getWitnessAddress().toByteArray());
      MetricsUtil.histogramUpdate(MetricsKey.NET_BLOCK_LATENCY, netTime);
      MetricsUtil.histogramUpdate(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress, netTime);
      if (netTime >= 3000) {
        MetricsUtil.counterInc(MetricsKey.NET_BLOCK_LATENCY + ".3S", 1L);
        MetricsUtil.counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".3S", 1L);
      } else if (netTime >= 2000) {
        MetricsUtil.counterInc(MetricsKey.NET_BLOCK_LATENCY + ".2S", 1L);
        MetricsUtil.counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".2S", 1L);
      } else if (netTime >= 1000) {
        MetricsUtil.counterInc(MetricsKey.NET_BLOCK_LATENCY + ".1S", 1L);
        MetricsUtil.counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".1S", 1L);
      }
    } catch (Exception e) {
      logger.warn("record block latency failed, {}, reason: {}.",
              block.getBlockId().toString(), e.getMessage());
    }
  }

  /**
   * get metrics info
   * @return MetricsInfo
   */
  public MetricsInfo getMetricsInfo() {
    return new MetricsInfo();
  }

}
