package org.tron.consensus.dpos;


import static org.tron.consensus.base.Constant.MAX_ACTIVE_WITNESS_NUM;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.utils.StringUtil;
import org.tron.consensus.ConsensusDelegate;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.store.AccountStore;
import org.tron.core.store.VotesStore;
import org.tron.core.store.WitnessStore;
import org.tron.protos.Protocol.Block;

@Slf4j(topic = "consensus")
@Component
public class MaintenanceManager {

  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Autowired
  private IncentiveManager incentiveManager;

  @Setter
  private DposService dposService;

  public void applyBlock(Block block) {
    long blockNum = block.getBlockHeader().getRawData().getNumber();
    long blockTime = block.getBlockHeader().getRawData().getTimestamp();
    boolean flag = consensusDelegate.getNextMaintenanceTime() <= blockTime;
    if (flag && blockNum != 1) {
      doMaintenance();
    }
    updateNextMaintenanceTime(block);
    consensusDelegate.saveStateFlag(flag ? 1 : 0);
  }

  private void updateNextMaintenanceTime(Block block) {
    long blockNum = block.getBlockHeader().getRawData().getNumber();
    long blockTime = block.getBlockHeader().getRawData().getTimestamp();
    long maintenanceTimeInterval = consensusDelegate.getMaintenanceTimeInterval();
    long currentMaintenanceTime = consensusDelegate.getNextMaintenanceTime();
    long round = (blockTime - currentMaintenanceTime) / maintenanceTimeInterval;
    long nextMaintenanceTime = currentMaintenanceTime + (round + 1) * maintenanceTimeInterval;
    consensusDelegate.saveNextMaintenanceTime(nextMaintenanceTime);
    logger.info("Update next maintenance time, blockNum:{}, blockTime:{}, currentMTime:{}, nextMTime:{}",
        blockNum,
        new DateTime(currentMaintenanceTime),
        new DateTime(blockTime),
        new DateTime(nextMaintenanceTime));
  }

  private void doMaintenance() {
    WitnessStore witnessStore = consensusDelegate.getWitnessStore();
    VotesStore votesStore = consensusDelegate.getVotesStore();
    AccountStore accountStore = consensusDelegate.getAccountStore();

    tryRemoveThePowerOfTheGr();

    Map<ByteString, Long> countWitness = countVote(votesStore);
    if (countWitness.isEmpty()) {
      logger.warn("No vote, no change to witness.");
      return;
    }

    List<ByteString> currentWits = consensusDelegate.getActiveWitnesses();

    List<ByteString> newWitnessAddressList = new ArrayList<>();
    witnessStore.getAllWitnesses()
        .forEach(witnessCapsule -> newWitnessAddressList.add(witnessCapsule.getAddress()));

    countWitness.forEach((address, voteCount) -> {
      WitnessCapsule witnessCapsule = witnessStore.get(StringUtil.createDbKey(address));
      if (witnessCapsule == null) {
        logger.warn("Witness capsule is null. address is {}",
            StringUtil.createReadableString(address));
        return;
      }
      AccountCapsule account = accountStore.get(StringUtil.createDbKey(address));
      if (account == null) {
        logger.warn("Witness account is null. address is {}",
            StringUtil.createReadableString(address));
        return;
      }
      witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
      witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      logger.info("address is {}  ,countVote is {}", witnessCapsule.createReadableString(),
          witnessCapsule.getVoteCount());
    });

    sortWitness(newWitnessAddressList);

    if (newWitnessAddressList.size() > MAX_ACTIVE_WITNESS_NUM) {
      consensusDelegate
          .saveActiveWitnesses(newWitnessAddressList.subList(0, MAX_ACTIVE_WITNESS_NUM));
    } else {
      consensusDelegate.saveActiveWitnesses(newWitnessAddressList);
    }

    incentiveManager.reward(newWitnessAddressList);

    List<ByteString> newWits = consensusDelegate.getActiveWitnesses();
    if (!CollectionUtils.isEqualCollection(currentWits, newWits)) {
      currentWits.forEach(address -> {
        WitnessCapsule witnessCapsule = consensusDelegate.getWitnesseByAddress(address);
        witnessCapsule.setIsJobs(false);
        witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      });
      newWits.forEach(address -> {
        WitnessCapsule witnessCapsule = consensusDelegate.getWitnesseByAddress(address);
        witnessCapsule.setIsJobs(true);
        witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      });
    }

    logger.info("Update witness success. \nbefore: {} \nafter: {}",
        StringUtil.getAddressStringList(currentWits),
        StringUtil.getAddressStringList(newWits));
  }

  private Map<ByteString, Long> countVote(VotesStore votesStore) {
    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    Iterator<Entry<byte[], VotesCapsule>> dbIterator = votesStore.iterator();
    long sizeCount = 0;
    while (dbIterator.hasNext()) {
      Entry<byte[], VotesCapsule> next = dbIterator.next();
      VotesCapsule votes = next.getValue();
      votes.getOldVotes().forEach(vote -> {
        //TODO validate witness //active_witness
        ByteString voteAddress = vote.getVoteAddress();
        long voteCount = vote.getVoteCount();
        if (countWitness.containsKey(voteAddress)) {
          countWitness.put(voteAddress, countWitness.get(voteAddress) - voteCount);
        } else {
          countWitness.put(voteAddress, -voteCount);
        }
      });
      votes.getNewVotes().forEach(vote -> {
        //TODO validate witness //active_witness
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
    logger.info("there is {} new votes in this epoch", sizeCount);
    return countWitness;
  }

  private void tryRemoveThePowerOfTheGr() {
    if (consensusDelegate.getRemoveThePowerOfTheGr() != 1) {
      return;
    }
    WitnessStore witnessStore = consensusDelegate.getWitnessStore();
    dposService.getGenesisBlock().getWitnesses().forEach(witness -> {
      WitnessCapsule witnessCapsule = witnessStore.get(witness.getAddress());
      witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() - witness.getVoteCount());
      witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    });
    consensusDelegate.saveRemoveThePowerOfTheGr(-1);
  }

  private void sortWitness(List<ByteString> list) {
    list.sort(Comparator.comparingLong((ByteString b) ->
        consensusDelegate.getWitnesseByAddress(b).getVoteCount())
        .reversed()
        .thenComparing(Comparator.comparingInt(ByteString::hashCode).reversed()));
  }

}
