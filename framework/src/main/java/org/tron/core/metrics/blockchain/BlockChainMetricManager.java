package org.tron.core.metrics.blockchain;

import com.codahale.metrics.Meter;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.metrics.MetricsInfo;
import org.tron.core.metrics.MetricsInfo.BlockchainInfo;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsService;

@Component
public class BlockChainMetricManager {

  private static Map<String, BlockChainInfo.Witness> witnessVersion = new HashMap<>();

  private static int currentVersion;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private MetricsService metricsService;


  public BlockchainInfo getBlockchainInfo() {
    return new BlockchainInfo();
  }

  public void applyBlcok(BlockCapsule block) {
    String witnessAddress = block.getWitnessAddress().toStringUtf8();
    int version = block.getInstance().getBlockHeader().getRawData().getVersion();
    currentVersion = Math.max(currentVersion, version);
    if (witnessVersion.containsKey(witnessAddress) &&
        witnessVersion.get(witnessAddress).getVersion() != version) {
      // just update version
      BlockChainInfo.Witness witness = witnessVersion.get(witnessAddress);
      witness.setVersion(version);
      witnessVersion.put(witnessAddress, witness);
    } else {
      List<WitnessCapsule> allWitness = chainBaseManager.getWitnessStore().getAllWitnesses();
      for (WitnessCapsule it : allWitness) {  // add new witness
        if (it.getAddress().toStringUtf8().equals(witnessAddress)) {
          BlockChainInfo.Witness witness = new BlockChainInfo.Witness(witnessAddress,
              it.getUrl(), version);
          witnessVersion.put(it.getAddress().toStringUtf8(), witness);
        }
      }
    }
  }

  public List<BlockChainInfo.Witness> getNoUpgradedSRList() {
    List<BlockChainInfo.Witness> noUpgradedWitness = new ArrayList<>();

    List<ByteString> address = chainBaseManager.getWitnessScheduleStore().getActiveWitnesses();
    for (ByteString it : address) {
      if (witnessVersion.containsKey(it.toStringUtf8()) &&
          witnessVersion.get(it.toStringUtf8()).getVersion() != currentVersion) {
        BlockChainInfo.Witness witness = witnessVersion.get(it.toStringUtf8());
        noUpgradedWitness.add(witness);
      }
    }
    return noUpgradedWitness;
  }

  public MetricsInfo.BlockchainInfo.TpsInfo getBlockProcessTime() {
    MetricsInfo.BlockchainInfo.TpsInfo blockProcessTime =
        new MetricsInfo.BlockchainInfo.TpsInfo();

    blockProcessTime.setMeanRate(getAvgBlockProcessTimeByGap(0));
    blockProcessTime.setOneMinuteRate(getAvgBlockProcessTimeByGap(1));
    blockProcessTime.setFiveMinuteRate(getAvgBlockProcessTimeByGap(5));
    blockProcessTime.setFifteenMinuteRate(getAvgBlockProcessTimeByGap(15));
    return blockProcessTime;
  }

  public MetricsInfo.BlockchainInfo.TpsInfo getTransactionRate() {
    Meter transactionRate = metricsService.getMeter(MetricsKey.BLOCKCHAIN_TPS);
    MetricsInfo.BlockchainInfo.TpsInfo tpsInfo =
        new MetricsInfo.BlockchainInfo.TpsInfo();
    tpsInfo.setMeanRate(transactionRate.getMeanRate());
    tpsInfo.setOneMinuteRate(transactionRate.getOneMinuteRate());
    tpsInfo.setFiveMinuteRate(transactionRate.getFiveMinuteRate());
    tpsInfo.setFifteenMinuteRate(transactionRate.getFifteenMinuteRate());
    return tpsInfo;
  }

  public List<MetricsInfo.BlockchainInfo.Witness> getNoUpgradedSR() {
    List<MetricsInfo.BlockchainInfo.Witness> witnesses = new ArrayList<>();
    for (BlockChainInfo.Witness it : getNoUpgradedSRList()) {
      MetricsInfo.BlockchainInfo.Witness noUpgradeSR =
          new MetricsInfo.BlockchainInfo.Witness();
      noUpgradeSR.setAddress(it.getAddress());
      noUpgradeSR.setVersion(it.getVersion());
      witnesses.add(noUpgradeSR);
    }
    return witnesses;
  }


  // gap: 1 minute, 5 minute, 15 minute, 0: avg for total block and time
  private double getAvgBlockProcessTimeByGap(int gap) {
    Meter meterBlockProcessTime =
        metricsService.getMeter(MetricsKey.BLOCKCHAIN_BLOCKPROCESS_TIME);
    Meter meterBlockTxCount = metricsService.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_COUNT);
    if (meterBlockTxCount.getCount() == 0) {
      return 0;
    }
    switch (gap) {
      case 0:
        return (meterBlockProcessTime.getCount() / (double) meterBlockTxCount.getCount());
      case 1:
        int gapMinuteTimeBlock =
            Math.round(Math.round(meterBlockProcessTime.getOneMinuteRate() * 60));
        int gapMinuteCount = Math.round(Math.round(meterBlockTxCount.getOneMinuteRate() * 60));
        return gapMinuteTimeBlock / (double) gapMinuteCount;
      case 5:
        int gapFiveTimeBlock =
            Math.round(Math.round(meterBlockProcessTime.getFiveMinuteRate() * gap * 60));
        int gapFiveTimeCount =
            Math.round(Math.round(meterBlockTxCount.getFiveMinuteRate() * gap * 60));
        return gapFiveTimeBlock / (double) gapFiveTimeCount;
      case 15:
        int gapFifteenTimeBlock =
            Math.round(Math.round(meterBlockProcessTime.getFifteenMinuteRate() * gap * 60));
        int gapFifteenTimeCount =
            Math.round(Math.round(meterBlockTxCount.getFifteenMinuteRate() * gap * 60));
        return gapFifteenTimeBlock / (double) gapFifteenTimeCount;

      default:
        return -1;
    }
  }


}
