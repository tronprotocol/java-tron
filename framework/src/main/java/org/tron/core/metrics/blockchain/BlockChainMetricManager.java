package org.tron.core.metrics.blockchain;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.Manager;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsService;
import org.tron.core.metrics.net.RateInfo;
import org.tron.protos.Protocol.BlockHeader;

@Component
public class BlockChainMetricManager {


  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private MetricsService metricsService;

  private Map<String, BlockHeader> witnessInfo = new ConcurrentHashMap<String, BlockHeader>();

  @Getter
  private Map<String, Long> dupWitnessBlockNum = new ConcurrentHashMap<String, Long>();

  public void init() {
    metricsService.setBlockChainMetricManager(this);
  }

  public BlockChainInfo getBlockChainInfo() {
    BlockChainInfo blockChainInfo = new BlockChainInfo();
    setBlockChainInfo(blockChainInfo);
    return blockChainInfo;
  }

  private void setBlockChainInfo(BlockChainInfo blockChain) {
    blockChain.setHeadBlockTimestamp(chainBaseManager.getHeadBlockTimeStamp());
    blockChain.setHeadBlockHash(dbManager.getDynamicPropertiesStore()
            .getLatestBlockHeaderHash().toString());

    RateInfo blockProcessTime = getBlockProcessTime();
    blockChain.setBlockProcessTime(blockProcessTime);
    blockChain.setSuccessForkCount(getSuccessForkCount());
    blockChain.setFailForkCount(getFailForkCount());
    blockChain.setHeadBlockNum((int) chainBaseManager.getHeadBlockNum());
    blockChain.setTransactionCacheSize(dbManager.getPendingTransactions().size());
    blockChain.setMissedTransactionCount(dbManager.getPendingTransactions().size()
            + dbManager.getRePushTransactions().size());

    RateInfo tpsInfo = getTransactionRate();
    blockChain.setTps(tpsInfo);

    List<WitnessInfo> witnesses = getSrList();

    blockChain.setWitnesses(witnesses);

    blockChain.setFailProcessBlockNum(metricsService.getFailProcessBlockNum());
    blockChain.setFailProcessBlockReason(metricsService.getFailProcessBlockReason());
    List<DupWitnessInfo> dupWitness = getDupWitness();
    blockChain.setDupWitness(dupWitness);
  }

  /**
   * apply block.
   * @param block BlockCapsule
   */
  public void applyBlock(BlockCapsule block) {
    long nowTime = System.currentTimeMillis();
    String witnessAddress = Hex.toHexString(block.getWitnessAddress().toByteArray());

    //witness info
    if (witnessInfo.containsKey(witnessAddress)) {
      BlockHeader old = witnessInfo.get(witnessAddress);
      if (old.getRawData().getNumber() == block.getNum()
          && Math.abs(old.getRawData().getTimestamp() - block.getTimeStamp()) < 3000) {
        metricsService.counterInc(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT + witnessAddress, 1);
        dupWitnessBlockNum.put(witnessAddress, block.getNum());
      }
    }
    witnessInfo.put(witnessAddress, block.getInstance().getBlockHeader());

    //latency
    long netTime = nowTime - block.getTimeStamp();
    metricsService.histogramUpdate(MetricsKey.NET_BLOCK_LATENCY, netTime);
    metricsService.histogramUpdate(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress, netTime);
    if (netTime >= 1000) {
      metricsService.counterInc(MetricsKey.NET_BLOCK_LATENCY + ".1S", 1L);
      metricsService.counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".1S", 1L);
      if (netTime >= 2000) {
        metricsService.counterInc(MetricsKey.NET_BLOCK_LATENCY + ".2S", 1L);
        metricsService.counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".2S",
                1L);
        if (netTime >= 3000) {
          metricsService.counterInc(MetricsKey.NET_BLOCK_LATENCY + ".3S", 1L);
          metricsService.counterInc(MetricsKey.NET_BLOCK_LATENCY_WITNESS + witnessAddress + ".3S",
                  1L);
        }
      }
    }

    //TPS
    if (block.getTransactions().size() > 0) {
      metricsService.meterMark(MetricsKey.BLOCKCHAIN_TPS, block.getTransactions().size());
    }
  }

  private List<WitnessInfo> getSrList() {
    List<WitnessInfo> witnessInfos = new ArrayList<>();

    List<WitnessCapsule> witnessCapsuleList = chainBaseManager.getWitnessStore().getAllWitnesses();
    for (WitnessCapsule witnessCapsule : witnessCapsuleList) {
      if (witnessCapsule.getIsJobs()) {
        String address = Hex.toHexString(witnessCapsule.getAddress().toByteArray());
        if (witnessInfo.containsKey(address)) {
          BlockHeader blockHeader = witnessInfo.get(address);
          WitnessInfo witness = new WitnessInfo(address, blockHeader.getRawData().getVersion());
          witnessInfos.add(witness);
        }
      }
    }
    return witnessInfos;
  }

  private RateInfo getBlockProcessTime() {
    RateInfo blockProcessTime = new RateInfo();
    blockProcessTime.setCount(metricsService.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_COUNT)
            .getCount());
    blockProcessTime.setMeanRate(getAvgBlockProcessTimeByGap(0));
    blockProcessTime.setOneMinuteRate(getAvgBlockProcessTimeByGap(1));
    blockProcessTime.setFiveMinuteRate(getAvgBlockProcessTimeByGap(5));
    blockProcessTime.setFifteenMinuteRate(getAvgBlockProcessTimeByGap(15));
    return blockProcessTime;
  }

  private RateInfo getTransactionRate() {
    Meter transactionRate = metricsService.getMeter(MetricsKey.BLOCKCHAIN_TPS);
    RateInfo tpsInfo = new RateInfo();
    tpsInfo.setCount(transactionRate.getCount());
    tpsInfo.setMeanRate(transactionRate.getMeanRate());
    tpsInfo.setOneMinuteRate(transactionRate.getOneMinuteRate());
    tpsInfo.setFiveMinuteRate(transactionRate.getFiveMinuteRate());
    tpsInfo.setFifteenMinuteRate(transactionRate.getFifteenMinuteRate());
    return tpsInfo;
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

  private int getSuccessForkCount() {
    return (int) metricsService.getMeter(MetricsKey.BLOCKCHAIN_SUCCESS_FORK_COUNT).getCount();
  }

  private int getFailForkCount() {
    return (int) metricsService.getMeter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount();
  }

  private List<DupWitnessInfo> getDupWitness() {
    List<DupWitnessInfo> dupWitnesses = new ArrayList<>();
    SortedMap<String, Counter> dupWitnessMap =
            metricsService.getCounters(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT);
    for (Map.Entry<String, Counter> entry : dupWitnessMap.entrySet()) {
      DupWitnessInfo dupWitness = new DupWitnessInfo();
      String witness = entry.getKey().substring(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT.length());
      long blockNum = dupWitnessBlockNum.get(witness);
      dupWitness.setAddress(witness);
      dupWitness.setBlockNum(blockNum);
      dupWitness.setCount((int)entry.getValue().getCount());
      dupWitnesses.add(dupWitness);
    }
    return dupWitnesses;
  }
}
