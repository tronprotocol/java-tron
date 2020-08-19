package org.tron.core.metrics.blockchain;

import com.codahale.metrics.Counter;
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
import org.tron.protos.Protocol;

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

    RateInfo blockProcessTime = MetricsUtil.getRateInfo(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME);
    blockChain.setBlockProcessTime(blockProcessTime);
    blockChain.setForkCount(getForkCount());
    blockChain.setFailForkCount(getFailForkCount());
    blockChain.setHeadBlockNum(chainBaseManager.getHeadBlockNum());
    blockChain.setTransactionCacheSize(dbManager.getPendingTransactions().size()
        + dbManager.getRePushTransactions().size());

    RateInfo missTx = MetricsUtil.getRateInfo(MetricsKey.BLOCKCHAIN_MISSED_TRANSACTION);
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

  public Protocol.MetricsInfo.BlockChainInfo getBlockChainProtoInfo() {
    Protocol.MetricsInfo.BlockChainInfo.Builder blockChainInfo =
        Protocol.MetricsInfo.BlockChainInfo.newBuilder();

    BlockChainInfo blockChain = getBlockChainInfo();
    blockChainInfo.setHeadBlockNum(blockChain.getHeadBlockNum());
    blockChainInfo.setHeadBlockTimestamp(blockChain.getHeadBlockTimestamp());
    blockChainInfo.setHeadBlockHash(blockChain.getHeadBlockHash());
    blockChainInfo.setFailProcessBlockNum(blockChain.getFailProcessBlockNum());
    blockChainInfo.setFailProcessBlockReason(blockChain.getFailProcessBlockReason());
    blockChainInfo.setForkCount(blockChain.getForkCount());
    blockChainInfo.setFailForkCount(blockChain.getFailForkCount());
    blockChainInfo.setTransactionCacheSize(blockChain.getTransactionCacheSize());
    RateInfo missTransaction = blockChain.getMissedTransaction();
    Protocol.MetricsInfo.RateInfo missTransactionInfo =
        missTransaction.toProtoEntity();
    blockChainInfo.setMissedTransaction(missTransactionInfo);

    RateInfo blockProcessTime = blockChain.getBlockProcessTime();
    Protocol.MetricsInfo.RateInfo blockProcessTimeInfo =
        blockProcessTime.toProtoEntity();
    blockChainInfo.setBlockProcessTime(blockProcessTimeInfo);
    RateInfo tps = blockChain.getTps();
    Protocol.MetricsInfo.RateInfo tpsInfo = tps.toProtoEntity();

    blockChainInfo.setTps(tpsInfo);
    for (WitnessInfo witness : blockChain.getWitnesses()) {
      Protocol.MetricsInfo.BlockChainInfo.Witness.Builder witnessInfo =
          Protocol.MetricsInfo.BlockChainInfo.Witness.newBuilder();
      witnessInfo.setAddress(witness.getAddress());
      witnessInfo.setVersion(witness.getVersion());
      blockChainInfo.addWitnesses(witnessInfo.build());
    }
    for (DupWitnessInfo dupWitness : blockChain.getDupWitness()) {
      Protocol.MetricsInfo.BlockChainInfo.DupWitness.Builder dupWitnessInfo =
          Protocol.MetricsInfo.BlockChainInfo.DupWitness.newBuilder();
      dupWitnessInfo.setAddress(dupWitness.getAddress());
      dupWitnessInfo.setBlockNum(dupWitness.getBlockNum());
      dupWitnessInfo.setCount(dupWitness.getCount());
      blockChainInfo.addDupWitness(dupWitnessInfo.build());
    }
    return blockChainInfo.build();

  }

  /**
   * apply block.
   *
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
        MetricsUtil.counterInc(MetricsKey.BLOCKCHAIN_DUP_WITNESS + witnessAddress);
        dupWitnessBlockNum.put(witnessAddress, block.getNum());
      }
    }
    witnessInfo.put(witnessAddress, block);

    //latency
    long netTime = nowTime - block.getTimeStamp();
    MetricsUtil.histogramUpdate(MetricsKey.NET_LATENCY, netTime);
    MetricsUtil.histogramUpdate(MetricsKey.NET_LATENCY_WITNESS + witnessAddress, netTime);
    if (netTime >= 3000) {
      MetricsUtil.counterInc(MetricsKey.NET_LATENCY + ".3S");
      MetricsUtil.counterInc(MetricsKey.NET_LATENCY_WITNESS + witnessAddress + ".3S");
    } else if (netTime >= 2000) {
      MetricsUtil.counterInc(MetricsKey.NET_LATENCY + ".2S");
      MetricsUtil.counterInc(MetricsKey.NET_LATENCY_WITNESS + witnessAddress + ".2S");
    } else if (netTime >= 1000) {
      MetricsUtil.counterInc(MetricsKey.NET_LATENCY + ".1S");
      MetricsUtil.counterInc(MetricsKey.NET_LATENCY_WITNESS + witnessAddress + ".1S");
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


  public int getForkCount() {
    return (int) MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_FORK_COUNT).getCount();
  }

  public int getFailForkCount() {
    return (int) MetricsUtil.getMeter(MetricsKey.BLOCKCHAIN_FAIL_FORK_COUNT).getCount();
  }

  private List<DupWitnessInfo> getDupWitness() {
    List<DupWitnessInfo> dupWitnesses = new ArrayList<>();
    SortedMap<String, Counter> dupWitnessMap =
        MetricsUtil.getCounters(MetricsKey.BLOCKCHAIN_DUP_WITNESS);
    for (Map.Entry<String, Counter> entry : dupWitnessMap.entrySet()) {
      DupWitnessInfo dupWitness = new DupWitnessInfo();
      String witness = entry.getKey().substring(MetricsKey.BLOCKCHAIN_DUP_WITNESS.length());
      long blockNum = dupWitnessBlockNum.get(witness);
      dupWitness.setAddress(witness);
      dupWitness.setBlockNum(blockNum);
      dupWitness.setCount((int) entry.getValue().getCount());
      dupWitnesses.add(dupWitness);
    }
    return dupWitnesses;
  }
}
