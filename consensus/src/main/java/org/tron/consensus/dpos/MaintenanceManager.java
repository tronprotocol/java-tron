package org.tron.consensus.dpos;

import static org.tron.common.utils.WalletUtil.getAddressStringList;
import static org.tron.core.config.Parameter.ChainConstant.TRX_PRECISION;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.consensus.ConsensusDelegate;
import org.tron.consensus.pbft.PbftManager;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.store.DelegationStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.VotesStore;
import org.tron.protos.Protocol.Vote;

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
            consensusDelegate.getNextMaintenanceTime());
      }
    }
    consensusDelegate.saveStateFlag(flag ? 1 : 0);
    //pbft block msg
    if (blockNum == 1) {
      nextMaintenanceTime = consensusDelegate.getNextMaintenanceTime();
    }
    pbftManager.blockPrePrepare(blockCapsule, nextMaintenanceTime);
  }

  private void updateWitnessValue(List<ByteString> srList) {
    srList.clear();
    srList.addAll(consensusDelegate.getActiveWitnesses());
  }

  public void doMaintenance() {
    VotesStore votesStore = consensusDelegate.getVotesStore();

    tryRemoveThePowerOfTheGr();

    DynamicPropertiesStore dynamicPropertiesStore = consensusDelegate.getDynamicPropertiesStore();
    DelegationStore delegationStore = consensusDelegate.getDelegationStore();
    boolean useNewRewardAlgorithm = dynamicPropertiesStore.useNewRewardAlgorithm();
    boolean useShareRewardAlgorithm = dynamicPropertiesStore.allowSlashVote();
    if (useNewRewardAlgorithm || useShareRewardAlgorithm) {
      long curCycle = dynamicPropertiesStore.getCurrentCycleNumber();
      consensusDelegate.getAllWitnesses().forEach(witness -> {
        if (useShareRewardAlgorithm) {
          delegationStore.accumulateWitnessNewVi(curCycle, witness.createDbKey(), witness.getTotalShares());
        } else {
          delegationStore.accumulateWitnessVi(curCycle, witness.createDbKey(),
                  witness.getVoteCount());
          delegationStore.accumulateWitnessOracleVi(curCycle, witness.createDbKey(),
                  witness.getVoteCount());
        }
      });
    }

    Map<ByteString, Vote> countWitness = countVote(votesStore, dynamicPropertiesStore.allowSlashVote());
    if (!countWitness.isEmpty()) {
      List<ByteString> currentWits = consensusDelegate.getActiveWitnesses();

      List<ByteString> newWitnessAddressList = new ArrayList<>();
      consensusDelegate.getAllWitnesses()
          .forEach(witnessCapsule -> newWitnessAddressList.add(witnessCapsule.getAddress()));

      countWitness.forEach((address, vote) -> {
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
        if (witnessCapsule.getTotalShares() <= 0) {
          witnessCapsule.setTotalShares(witnessCapsule.getVoteCount() * TRX_PRECISION + vote.getShares());
        } else {
          witnessCapsule.setTotalShares(witnessCapsule.getTotalShares() + vote.getShares());
        }
        witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + vote.getVoteCount());
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

    if (dynamicPropertiesStore.allowChangeDelegation()) {
      long nextCycle = dynamicPropertiesStore.getCurrentCycleNumber() + 1;
      dynamicPropertiesStore.saveCurrentCycleNumber(nextCycle);
      consensusDelegate.getAllWitnesses().forEach(witness -> {
        delegationStore.setBrokerage(nextCycle, witness.createDbKey(),
            delegationStore.getBrokerage(witness.createDbKey()));
        delegationStore.setWitnessVote(nextCycle, witness.createDbKey(), witness.getVoteCount());
      });
    }
  }

  private Map<ByteString, Vote> countVote(VotesStore votesStore, boolean allowSlashVote) {
    final Map<ByteString, Vote> countWitness = Maps.newHashMap();
    final Map<ByteString, Long> oldCountWitness = Maps.newHashMap();
    Iterator<Entry<byte[], VotesCapsule>> dbIterator = votesStore.iterator();
    long sizeCount = 0;
    while (dbIterator.hasNext()) {
      Entry<byte[], VotesCapsule> next = dbIterator.next();
      VotesCapsule votes = next.getValue();
      votes.getOldVotes().forEach(vote -> {
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        long shareCount = vote.getShares() == 0 ? (vote.getVoteCount() * TRX_PRECISION) : vote.getShares();
        if (countWitness.containsKey(voteAddress)) {
          Vote witnessVote = countWitness.get(voteAddress);
          witnessVote = witnessVote.toBuilder().setVoteCount(witnessVote.getVoteCount() - voteCount)
                  .setShares(witnessVote.getShares() - shareCount).build();
          countWitness.put(voteAddress, witnessVote);
        } else {
          Vote witnessVote = Vote.newBuilder().setVoteCount(-voteCount).setShares(-shareCount).build();
          countWitness.put(voteAddress, witnessVote);
        }
        if (oldCountWitness.containsKey(voteAddress)) {
          oldCountWitness.put(voteAddress, oldCountWitness.get(voteAddress) + voteCount);
        } else {
          oldCountWitness.put(voteAddress, voteCount);
        }
      });

      votes.getNewVotes().forEach(vote -> {
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        long shareCount = vote.getShares() == 0 ? (vote.getVoteCount() * TRX_PRECISION) : vote.getShares();
        if (countWitness.containsKey(voteAddress)) {
          Vote witnessVote = countWitness.get(voteAddress);
          witnessVote = witnessVote.toBuilder().setVoteCount(witnessVote.getVoteCount() + voteCount)
                  .setShares(witnessVote.getShares() + shareCount).build();
          countWitness.put(voteAddress, witnessVote);
        } else {
          Vote witnessVote = Vote.newBuilder().setVoteCount(voteCount).setShares(shareCount).build();
          countWitness.put(voteAddress, witnessVote);
        }
      });
      sizeCount++;
      votesStore.delete(next.getKey());
    }
    logger.info("There is {} new votes in this epoch", sizeCount);

    if (allowSlashVote) {
      List<ByteString> slashingWitnessList = slashAndResetMissCounters();
      slashingWitnessList.forEach(address -> {
        byte[] witnessAddress = address.toByteArray();
        WitnessCapsule witnessCapsule = consensusDelegate.getWitness(witnessAddress);
        if (witnessCapsule == null) {
          logger.warn("Witness capsule is null. address is {}", Hex.toHexString(witnessAddress));
          return;
        }
        // todo fraction
        long voteCount = witnessCapsule.getVoteCount() / 10000;
        if (oldCountWitness.containsKey(address)) {
          voteCount = (witnessCapsule.getVoteCount() - oldCountWitness.get(address)) / 10000;
        }
//        if (countWitness.containsKey(address)) {
//          Vote witnessVote = countWitness.get(address);
//          witnessVote = witnessVote.toBuilder().setVoteCount(witnessVote.getVoteCount() - voteCount).build();
//          countWitness.put(address, witnessVote);
//        } else {
//          Vote witnessVote = Vote.newBuilder().setVoteCount(-voteCount).build();
//          countWitness.put(address, witnessVote);
//        }
        consensusDelegate.getSlashService().slashWitness(witnessAddress, voteCount, true);
      });
    }

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

  private List<ByteString> slashAndResetMissCounters() {
    final List<ByteString> slashingWitnessList = new ArrayList<>();
    DynamicPropertiesStore dynamicPropertiesStore = consensusDelegate.getDynamicPropertiesStore();
    long SlashWindow = 28; // todo
    if ((dynamicPropertiesStore.getCurrentCycleNumber() + 1) % SlashWindow == 0) {
      // todo witness miss count
    }

    return slashingWitnessList;
  }

}
