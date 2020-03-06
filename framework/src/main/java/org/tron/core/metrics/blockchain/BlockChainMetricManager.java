package org.tron.core.metrics.blockchain;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.ChainBaseManager;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;
import org.tron.core.metrics.MetricsKey;
import org.tron.core.metrics.MetricsUtil;
import org.tron.core.metrics.net.RateInfo;

@Component
public class BlockChainMetricManager {


  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  private Map<String, BlockCapsule> witnessInfo = new ConcurrentHashMap<String, BlockCapsule>();

  @Getter
  private Map<String, Long> dupWitnessBlockNum = new ConcurrentHashMap<String, Long>();
  @Setter
  private long failProcessBlockNum = 0;
  @Setter
  private String failProcessBlockReason = "";

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
    blockChain.setForkCount(getForkCount());
    blockChain.setFailForkCount(getFailForkCount());
    blockChain.setHeadBlockNum((int) chainBaseManager.getHeadBlockNum());
    blockChain.setTransactionCacheSize(dbManager.getPendingTransactions().size()
        + dbManager.getRePushTransactions().size());

    RateInfo missTx = MetricsUtil.getRateInfo(MetricsKey.BLOCKCHAIN_MISS_TRANSACTION);
    blockChain.setMissedTransaction(missTx);

    RateInfo tpsInfo = MetricsUtil.getRateInfo(MetricsKey.BLOCKCHAIN_TPS);
    blockChain.setTps(tpsInfo);

    List<WitnessInfo> witnesses = getSrList();

    blockChain.setWitnesses(witnesses);

    blockChain.setFailProcessBlockNum(failProcessBlockNum);
    blockChain.setFailProcessBlockReason(failProcessBlockReason);
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
      BlockCapsule oldBlock = witnessInfo.get(witnessAddress);
      if ((!oldBlock.getBlockId().equals(block.getBlockId()))
              && oldBlock.getTimeStamp() == block.getTimeStamp()) {
        MetricsUtil.counterInc(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT + witnessAddress, 1);
        dupWitnessBlockNum.put(witnessAddress, block.getNum());
      }
    }
    witnessInfo.put(witnessAddress, block);

    //latency
    long netTime = nowTime - block.getTimeStamp();
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

    //TPS
    if (block.getTransactions().size() > 0) {
      MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_TPS, block.getTransactions().size());
    }
  }

  private List<WitnessInfo> getSrList() {
    List<WitnessInfo> witnessInfos = new ArrayList<>();

    List<ByteString> witnessList = chainBaseManager.getWitnessScheduleStore().getActiveWitnesses();
    for (ByteString witnessAddress : witnessList) {
      String address = Hex.toHexString(witnessAddress.toByteArray());
      if (witnessInfo.containsKey(address)) {
        BlockCapsule block = witnessInfo.get(address);
        WitnessInfo witness = new WitnessInfo(address,
                block.getInstance().getBlockHeader().getRawData().getVersion());
        witnessInfos.add(witness);
      }
    }
    return witnessInfos;
  }

  private RateInfo getBlockProcessTime() {
    RateInfo blockProcessTime = new RateInfo();
    blockProcessTime.setCount(MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_COUNT)
            .getCount());
    blockProcessTime.setMeanRate(getAvgBlockProcessTimeByGap(0));
    blockProcessTime.setOneMinuteRate(getAvgBlockProcessTimeByGap(1));
    blockProcessTime.setFiveMinuteRate(getAvgBlockProcessTimeByGap(5));
    blockProcessTime.setFifteenMinuteRate(getAvgBlockProcessTimeByGap(15));
    return blockProcessTime;
  }


  // gap: 1 minute, 5 minute, 15 minute, 0: avg for total block and time
  private double getAvgBlockProcessTimeByGap(int gap) {
    Meter meterBlockProcessTime =
        MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCKPROCESS_TIME);
    Meter meterBlockTxCount = MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_BLOCK_COUNT);
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

  public int getForkCount() {
    return (int) MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN__FORK_COUNT).getCount();
  }

  public int getFailForkCount() {
    return (int) MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount();
  }

  private List<DupWitnessInfo> getDupWitness() {
    List<DupWitnessInfo> dupWitnesses = new ArrayList<>();
    SortedMap<String, Counter> dupWitnessMap =
            MetricsUtil.getCounters(MetricsKey.BLOCKCHAIN_DUP_WITNESS_COUNT);
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
