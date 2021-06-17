package org.tron.consensus.dpos;

import static org.tron.common.utils.WalletUtil.getAddressStringList;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.*;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.AuctionConfigParser;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Pair;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.pbft.PbftManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.CommonDataBase;
import org.tron.core.db.CrossRevokingStore;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.VotesStore;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.PBFTCommitResult;
import org.tron.protos.Protocol.PBFTMessage;
import org.tron.protos.Protocol.PBFTMessage.Raw;
import org.tron.protos.contract.BalanceContract.CrossChainInfo;
import org.tron.protos.contract.CrossChain;

@Slf4j(topic = "consensus")
@Component
public class MaintenanceManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private IncentiveManager incentiveManager;

  @Setter
  private DposService dposService;

  @Setter
  private PbftManager pbftManager;

  @Getter
  private final List<ByteString> beforeWitness = new ArrayList<>();
  @Getter
  private final List<ByteString> currentWitness = new ArrayList<>();
  @Getter
  private long beforeMaintenanceTime;

  public void init() {
    currentWitness.addAll(consensusDelegate.getActiveWitnesses());
    beforeWitness.addAll(currentWitness);
  }

  public void applyBlock(BlockCapsule blockCapsule) {
    long blockNum = blockCapsule.getNum();
    long blockTime = blockCapsule.getTimeStamp();
    long nextMaintenanceTime = consensusDelegate.getNextMaintenanceTime();
    boolean flag = consensusDelegate.getNextMaintenanceTime() <= blockTime;
    if (flag) {
      if (blockNum != 1) {
        updateWitnessValue(beforeWitness);
        beforeMaintenanceTime = nextMaintenanceTime;
        doMaintenance();
        updateWitnessValue(currentWitness);
      }
      consensusDelegate.updateNextMaintenanceTime(blockTime);
      if (blockNum != 1) {
        //pbft sr msg
        pbftManager.srPrePrepare(blockCapsule, currentWitness,
            consensusDelegate.getNextMaintenanceTime(), beforeWitness);
      }
    }
    consensusDelegate.saveStateFlag(flag ? 1 : 0);
    //pbft block msg
    if (blockNum == 1) {
      nextMaintenanceTime = consensusDelegate.getNextMaintenanceTime();
    } else {
      long maintenanceTimeInterval =
              consensusDelegate.getDynamicPropertiesStore().getMaintenanceTimeInterval();
      nextMaintenanceTime = (blockTime / maintenanceTimeInterval + 1) * maintenanceTimeInterval;
      if (blockTime % maintenanceTimeInterval == 0) {
        nextMaintenanceTime = nextMaintenanceTime - maintenanceTimeInterval;
        nextMaintenanceTime = nextMaintenanceTime < 0 ? 0 : nextMaintenanceTime;
      }
    }
    if (flag) {
      pbftManager.blockPrePrepare(blockCapsule, nextMaintenanceTime, beforeWitness);
    } else {
      pbftManager.blockPrePrepare(blockCapsule, nextMaintenanceTime, currentWitness);
    }
  }

  private void updateWitnessValue(List<ByteString> srList) {
    srList.clear();
    srList.addAll(consensusDelegate.getActiveWitnesses());
  }

  public void doMaintenance() {
    VotesStore votesStore = consensusDelegate.getVotesStore();

    tryRemoveThePowerOfTheGr();

    Map<ByteString, Long> countWitness = countVote(votesStore);
    if (!countWitness.isEmpty()) {
      List<ByteString> currentWits = consensusDelegate.getActiveWitnesses();

      List<ByteString> newWitnessAddressList = new ArrayList<>();
      consensusDelegate.getAllWitnesses()
          .forEach(witnessCapsule -> newWitnessAddressList.add(witnessCapsule.getAddress()));

      countWitness.forEach((address, voteCount) -> {
        byte[] witnessAddress = address.toByteArray();
        WitnessCapsule witnessCapsule = consensusDelegate.getWitness(witnessAddress);
        if (witnessCapsule == null) {
          logger.warn("Witness capsule is null. address is {}", Hex.toHexString(witnessAddress));
          return;
        }
        AccountCapsule account = consensusDelegate.getAccount(witnessAddress);
        if (account == null) {
          logger.warn("Witness account is null. address is {}", Hex.toHexString(witnessAddress));
          return;
        }
        witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
        consensusDelegate.saveWitness(witnessCapsule);
        logger.info("address is {} , countVote is {}", witnessCapsule.createReadableString(),
            witnessCapsule.getVoteCount());
      });

      dposService.updateWitness(newWitnessAddressList);

      incentiveManager.reward(newWitnessAddressList);

      List<ByteString> newWits = consensusDelegate.getActiveWitnesses();
      if (!CollectionUtils.isEqualCollection(currentWits, newWits)) {
        currentWits.forEach(address -> {
          WitnessCapsule witnessCapsule = consensusDelegate.getWitness(address.toByteArray());
          witnessCapsule.setIsJobs(false);
          consensusDelegate.saveWitness(witnessCapsule);
        });
        newWits.forEach(address -> {
          WitnessCapsule witnessCapsule = consensusDelegate.getWitness(address.toByteArray());
          witnessCapsule.setIsJobs(true);
          consensusDelegate.saveWitness(witnessCapsule);
        });
      }

      logger.info("Update witness success. \nbefore: {} \nafter: {}",
          getAddressStringList(currentWits),
          getAddressStringList(newWits));
    }

    DynamicPropertiesStore dynamicPropertiesStore = consensusDelegate.getDynamicPropertiesStore();
    DelegationStore delegationStore = consensusDelegate.getDelegationStore();
    if (dynamicPropertiesStore.allowChangeDelegation()) {
      long nextCycle = dynamicPropertiesStore.getCurrentCycleNumber() + 1;
      dynamicPropertiesStore.saveCurrentCycleNumber(nextCycle);
      consensusDelegate.getAllWitnesses().forEach(witness -> {
        delegationStore.setBrokerage(nextCycle, witness.createDbKey(),
            delegationStore.getBrokerage(witness.createDbKey()));
        delegationStore.setWitnessVote(nextCycle, witness.createDbKey(), witness.getVoteCount());
      });
    }

    // update parachains
    long currentBlockHeaderTimestamp = dynamicPropertiesStore.getLatestBlockHeaderTimestamp();
    List<Long> auctionRoundList = dynamicPropertiesStore.listAuctionConfigs();
    long minAuctionVoteCount = dynamicPropertiesStore.getMinAuctionVoteCount();
    auctionRoundList.forEach(value -> {
      CrossChain.AuctionRoundContract roundInfo = AuctionConfigParser.parseAuctionConfig(value);
      if (roundInfo != null && roundInfo.getRound() > 0
          && (roundInfo.getEndTime() * 1000) <= currentBlockHeaderTimestamp) {
        CrossRevokingStore crossRevokingStore = consensusDelegate.getCrossRevokingStore();
        if (currentBlockHeaderTimestamp
            <= (roundInfo.getEndTime() + roundInfo.getDuration() * 86400) * 1000) {
          if (crossRevokingStore.getParaChainList(roundInfo.getRound()).isEmpty()) {
            // set parachains
            List<Pair<Long, Long>> eligibleChainLists = crossRevokingStore
                    .getChainVoteCountList(roundInfo.getRound(), minAuctionVoteCount);
            updateParaChains(eligibleChainLists, roundInfo);
          }
        } else {
          crossRevokingStore.deleteParaChains(roundInfo.getRound());
          crossRevokingStore.deleteParaChainRegisterNums(roundInfo.getRound());
        }
      }
    });

  }

  private void updateParaChains(List<Pair<Long, Long>> eligibleChainLists,
                                CrossChain.AuctionRoundContract roundInfo) {
    CrossRevokingStore crossRevokingStore = consensusDelegate.getCrossRevokingStore();
    List<Long> registerNums = new LinkedList<>();
    List<String> chainIds = new LinkedList<>();
    for (Pair<Long, Long> voteInfo : eligibleChainLists) {
      if (chainIds.size() >= roundInfo.getSlotCount()) {
        break;
      }
      try {
        byte[] chainInfoData = crossRevokingStore.getChainInfo(voteInfo.getKey());
        if (ByteArray.isEmpty(chainInfoData)) {
          return;
        }
        CrossChainInfo crossChainInfo = CrossChainInfo.parseFrom(chainInfoData);
        String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
        if (!chainIds.contains(chainId)) {
          chainIds.add(chainId);
          registerNums.add(voteInfo.getKey());
        }
      } catch (InvalidProtocolBufferException e) {
        logger.error("chain {} get the info fail!", voteInfo.getKey(), e);
      }
    }

    crossRevokingStore.updateParaChainRegisterNums(roundInfo.getRound(), registerNums);
    crossRevokingStore.updateParaChains(roundInfo.getRound(), chainIds);
    crossRevokingStore.updateParaChainsHistory(chainIds);

    setChainInfo(registerNums);
  }

  private void setChainInfo(List<Long> registerNums) {
    CrossRevokingStore crossRevokingStore = consensusDelegate.getCrossRevokingStore();
    CommonDataBase commonDataBase = consensusDelegate.getChainBaseManager().getCommonDataBase();
    registerNums.forEach(registerNum -> {
      try {
        byte[] chainInfoData = crossRevokingStore.getChainInfo(registerNum);
        if (ByteArray.isEmpty(chainInfoData)) {
          return;
        }
        CrossChainInfo crossChainInfo = CrossChainInfo.parseFrom(chainInfoData);
        String chainId = ByteArray.toHexString(crossChainInfo.getChainId().toByteArray());
        if (crossChainInfo.getBeginSyncHeight() <= commonDataBase
            .getLatestHeaderBlockNum(chainId)) {
          return;
        }
        commonDataBase.saveProxyAddress(chainId,
            ByteArray.toHexString(crossChainInfo.getProxyAddress().toByteArray()));
        commonDataBase.saveLatestHeaderBlockNum(chainId,
                crossChainInfo.getBeginSyncHeight() - 1, false);
        commonDataBase.saveLatestBlockHeaderHash(chainId,
            ByteArray.toHexString(crossChainInfo.getParentBlockHash().toByteArray()));
        commonDataBase.saveChainMaintenanceTimeInterval(chainId,
            crossChainInfo.getMaintenanceTimeInterval());
        long round = crossChainInfo.getBlockTime() / crossChainInfo.getMaintenanceTimeInterval();
        long epoch = (round + 1) * crossChainInfo.getMaintenanceTimeInterval();
        if (crossChainInfo.getBlockTime() % crossChainInfo.getMaintenanceTimeInterval() == 0) {
          epoch = epoch - crossChainInfo.getMaintenanceTimeInterval();
          epoch = epoch < 0 ? 0 : epoch;
        }
        Protocol.SRL.Builder srlBuilder = Protocol.SRL.newBuilder();
        srlBuilder.addAllSrAddress(crossChainInfo.getSrListList());
        PBFTMessage.Raw pbftMsgRaw = Raw.newBuilder().setData(srlBuilder.build().toByteString())
            .setEpoch(epoch).build();
        PBFTCommitResult.Builder builder = PBFTCommitResult.newBuilder();
        builder.setData(pbftMsgRaw.toByteString());
        commonDataBase.saveSRL(chainId, epoch, builder.build());
        commonDataBase.saveCrossNextMaintenanceTime(chainId, epoch);
        int agreeNodeCount = crossChainInfo.getSrListList().size() * 2 / 3 + 1;
        commonDataBase.saveAgreeNodeCount(chainId, agreeNodeCount);
      } catch (InvalidProtocolBufferException e) {
        logger.error("chain {} get the info fail!", registerNum, e);
      }
    });
  }

  private Map<ByteString, Long> countVote(VotesStore votesStore) {
    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    Iterator<Entry<byte[], VotesCapsule>> dbIterator = votesStore.iterator();
    long sizeCount = 0;
    while (dbIterator.hasNext()) {
      Entry<byte[], VotesCapsule> next = dbIterator.next();
      VotesCapsule votes = next.getValue();
      votes.getOldVotes().forEach(vote -> {
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        if (countWitness.containsKey(voteAddress)) {
          countWitness.put(voteAddress, countWitness.get(voteAddress) - voteCount);
        } else {
          countWitness.put(voteAddress, -voteCount);
        }
      });
      votes.getNewVotes().forEach(vote -> {
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        if (countWitness.containsKey(voteAddress)) {
          countWitness.put(voteAddress, countWitness.get(voteAddress) + voteCount);
        } else {
          countWitness.put(voteAddress, voteCount);
        }
      });
      sizeCount++;
      votesStore.delete(next.getKey());
    }
    logger.info("There is {} new votes in this epoch", sizeCount);
    return countWitness;
  }

  private void tryRemoveThePowerOfTheGr() {
    if (consensusDelegate.getRemoveThePowerOfTheGr() != 1) {
      return;
    }
    dposService.getGenesisBlock().getWitnesses().forEach(witness -> {
      WitnessCapsule witnessCapsule = consensusDelegate.getWitness(witness.getAddress());
      witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() - witness.getVoteCount());
      consensusDelegate.saveWitness(witnessCapsule);
    });
    consensusDelegate.saveRemoveThePowerOfTheGr(-1);
  }

}
