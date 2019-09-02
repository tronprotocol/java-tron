package org.tron.consensus.dpos;


import static org.tron.core.config.args.Parameter.ChainConstant.MAX_ACTIVE_WITNESS_NUM;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
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
    if (flag) {
      if (blockNum != 1) {
        doMaintenance();
      }
      consensusDelegate.updateNextMaintenanceTimes(blockTime);
    }
    consensusDelegate.saveStateFlag(flag ? 1 : 0);
  }

  public void doMaintenance() {
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
      byte[] witnessAddress = address.toByteArray();
      WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
      if (witnessCapsule == null) {
        logger.warn("Witness capsule is null. address is {}", Hex.toHexString(witnessAddress));
        return;
      }
      AccountCapsule account = accountStore.get(witnessAddress);
      if (account == null) {
        logger.warn("Witness account is null. address is {}", Hex.toHexString(witnessAddress));
        return;
      }
      witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
      witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      logger.info("address is {} , countVote is {}", witnessCapsule.createReadableString(),
          witnessCapsule.getVoteCount());
    });

    dposService.sortWitness(newWitnessAddressList);

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
    WitnessStore witnessStore = consensusDelegate.getWitnessStore();
    dposService.getGenesisBlock().getWitnesses().forEach(witness -> {
      WitnessCapsule witnessCapsule = witnessStore.get(witness.getAddress());
      witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() - witness.getVoteCount());
      witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
    });
    consensusDelegate.saveRemoveThePowerOfTheGr(-1);
  }

}
