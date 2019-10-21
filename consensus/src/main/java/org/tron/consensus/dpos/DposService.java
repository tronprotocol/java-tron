package org.tron.consensus.dpos;


import static org.tron.consensus.base.Constant.SOLIDIFIED_THRESHOLD;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.joda.time.DateTime;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.base.BlockHandle;
import org.tron.consensus.base.ConsensusInterface;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.GenesisBlock;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.SrList;

@Slf4j(topic = "consensus")
@Component
public class DposService implements ConsensusInterface {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private DposTask dposTask;

  @Autowired
  private DposSlot dposSlot;

  @Autowired
  private IncentiveManager incentiveManager;

  @Autowired
  private StateManager stateManager;

  @Autowired
  private StatisticManager statisticManager;

  @Autowired
  private MaintenanceManager maintenanceManager;

  @Getter
  @Setter
  private volatile boolean needSyncCheck;
  @Getter
  private volatile boolean enable;
  @Getter
  private int minParticipationRate;
  @Getter
  private int blockProduceTimeoutPercent;
  @Getter
  private long genesisBlockTime;
  @Getter
  private BlockHandle blockHandle;
  @Getter
  private GenesisBlock genesisBlock;
  @Getter
  private Map<ByteString, Miner> miners = new HashMap<>();

  @Override
  public void start(Param param) {
    this.enable = param.isEnable();
    this.needSyncCheck = param.isNeedSyncCheck();
    this.minParticipationRate = param.getMinParticipationRate();
    this.blockProduceTimeoutPercent = param.getBlockProduceTimeoutPercent();
    this.blockHandle = param.getBlockHandle();
    this.genesisBlock = param.getGenesisBlock();
    this.genesisBlockTime = Long.parseLong(param.getGenesisBlock().getTimestamp());
    param.getMiners().forEach(miner -> miners.put(miner.getWitnessAddress(), miner));

    dposTask.setDposService(this);
    dposSlot.setDposService(this);
    stateManager.setDposService(this);
    maintenanceManager.setDposService(this);

    if (consensusDelegate.getLatestBlockHeaderNumber() == 0) {
      List<ByteString> witnesses = new ArrayList<>();
      consensusDelegate.getAllWitnesses().forEach(witnessCapsule -> {
        if (witnessCapsule.getIsJobs()) {
          witnesses.add(witnessCapsule.getAddress());
        }
      });
      sortWitness(witnesses);
      consensusDelegate.saveActiveWitnesses(witnesses);
      maintenanceManager.init();
    }

    dposTask.init();
  }

  @Override
  public void stop() {
    dposTask.stop();
  }

  @Override
  public void receiveBlock(BlockCapsule blockCapsule) {
    stateManager.receiveBlock(blockCapsule);
  }

  @Override
  public boolean validBlock(BlockCapsule blockCapsule) {
    if (consensusDelegate.getLatestBlockHeaderNumber() == 0) {
      return true;
    }
    ByteString witnessAddress = blockCapsule.getWitnessAddress();
    long timeStamp = blockCapsule.getTimeStamp();
    long bSlot = dposSlot.getAbSlot(timeStamp);
    long hSlot = dposSlot.getAbSlot(consensusDelegate.getLatestBlockHeaderTimestamp());
    if (bSlot <= hSlot) {
      logger.warn("ValidBlock failed: bSlot: {} <= hSlot: {}", bSlot, hSlot);
      return false;
    }

    long slot = dposSlot.getSlot(timeStamp);
    final ByteString scheduledWitness = dposSlot.getScheduledWitness(slot);
    if (!scheduledWitness.equals(witnessAddress)) {
      logger.warn("ValidBlock failed: sWitness: {}, bWitness: {}, bTimeStamp: {}, slot: {}",
          ByteArray.toHexString(scheduledWitness.toByteArray()),
          ByteArray.toHexString(witnessAddress.toByteArray()), new DateTime(timeStamp), slot);
      return false;
    }

    return validSrList(blockCapsule.getInstance());
  }

  private boolean validSrList(Block block) {
    //valid sr list
    long startTime = System.currentTimeMillis();
    SrList srList = block.getBlockHeader().getRawData().getCurrentSrList();
    List<ByteString> addressList = srList.getCurrentSrListList();
    List<ByteString> preCycleSrSignList = srList.getPreSrsSignatureList();
    if (addressList.size() != 0) {
      long cycle = block.getBlockHeader().getRawData().getCurrentSrList().getCycle();
      if (cycle != consensusDelegate.getCurrentCycleNumber()) {
        return false;
      }
      if (cycle <= consensusDelegate.getSrListCurrentCycle()) {
        return false;
      }
      List<ByteString> localAddressList = consensusDelegate.getActiveWitnesses();
      Set<ByteString> addressSet = addressList.stream()
          .map(bytes -> ByteString.copyFrom(Hex.decode(bytes.toStringUtf8())))
          .collect(Collectors.toSet());
      Set<ByteString> preCycleSrSignSet = Sets.newHashSet(preCycleSrSignList);
      if (addressList.size() != localAddressList.size()) {
        return false;
      }
      if (preCycleSrSignSet.size() < Param.getInstance().getAgreeNodeCount()) {
        return false;
      }
      for (ByteString bs : addressList) {
        logger.info("addressList:{}", bs.toStringUtf8());
      }
      for (ByteString bs : localAddressList) {
        logger.info("localAddressList:{}", Hex.toHexString(bs.toByteArray()));
      }
      if (!SetUtils.isEqualSet(Sets.newHashSet(localAddressList), addressSet)) {
        return false;
      }
      List<String> addressStingList = addressList.stream()
          .map(sr -> sr.toStringUtf8()).collect(Collectors.toList());
      ByteString data = ByteString.copyFromUtf8(JSON.toJSONString(addressStingList));
      byte[] dataHash = Sha256Hash.hash(data.toByteArray());
      for (ByteString sign : preCycleSrSignList) {
        try {
          byte[] srAddress = ECKey.signatureToAddress(dataHash,
              TransactionCapsule.getBase64FromByteString(sign));
          if (!addressSet.contains(ByteString.copyFrom(srAddress))) {
            return false;
          }
          preCycleSrSignSet.remove(sign);
        } catch (SignatureException e) {
          logger.error("block {} valid sr list sign fail!",
              block.getBlockHeader().getRawData().getNumber(), e);
          return false;
        }
      }
      if (preCycleSrSignSet.size() != 0) {
        return false;
      }
      consensusDelegate.saveSrListCurrentCycle(cycle);
    }
    logger.info("block {} validSrList spend time : {}",
        block.getBlockHeader().getRawData().getNumber(), (System.currentTimeMillis() - startTime));
    return true;
  }

  @Override
  public boolean applyBlock(BlockCapsule blockCapsule) {
    statisticManager.applyBlock(blockCapsule);
    incentiveManager.applyBlock(blockCapsule);
    maintenanceManager.applyBlock(blockCapsule);
    updateSolidBlock();
    return true;
  }

  private void updateSolidBlock() {
    List<Long> numbers = consensusDelegate.getActiveWitnesses().stream()
        .map(address -> consensusDelegate.getWitness(address.toByteArray()).getLatestBlockNum())
        .sorted()
        .collect(Collectors.toList());
    long size = consensusDelegate.getActiveWitnesses().size();
    int position = (int) (size * (1 - SOLIDIFIED_THRESHOLD * 1.0 / 100));
    long newSolidNum = numbers.get(position);
    long oldSolidNum = consensusDelegate.getLatestSolidifiedBlockNum();
    if (newSolidNum < oldSolidNum) {
      logger.warn("Update solid block number failed, new: {} < old: {}", newSolidNum, oldSolidNum);
      return;
    }
    consensusDelegate.saveLatestSolidifiedBlockNum(newSolidNum);
    logger.info("Update solid block number to {}", newSolidNum);
  }

  public void sortWitness(List<ByteString> list) {
    list.sort(Comparator.comparingLong((ByteString b) ->
        consensusDelegate.getWitness(b.toByteArray()).getVoteCount())
        .reversed()
        .thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
  }

}
