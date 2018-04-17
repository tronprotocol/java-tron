package org.tron.core.witness;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.RandomGenerator;
import org.tron.common.utils.StringUtil;
import org.tron.common.utils.Time;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.db.WitnessStore;
import org.tron.core.exception.HeaderNotFound;

@Slf4j
public class WitnessController {

  @Setter
  private Manager manager;
  private volatile List<WitnessCapsule> wits = new ArrayList<>();

  @Getter
  @Setter
  protected List<WitnessCapsule> shuffledWitnessStates;

  private ReadWriteLock witsLock = new ReentrantReadWriteLock();
  private Lock witsRead = witsLock.readLock();
  private Lock witsWrite = witsLock.writeLock();

  public static WitnessController createInstance(Manager manager) {
    WitnessController instance = new WitnessController();
    instance.setManager(manager);
    return instance;
  }


  public void initWits() {
    getWitnesses().clear();
    manager.getWitnessStore().getAllWitnesses().forEach(witnessCapsule -> {
      if (witnessCapsule.getIsJobs()) {
        addWitness(witnessCapsule);
      }
    });
    sortWitness();
    this.setShuffledWitnessStates(getWitnesses());
  }

  // witness
  public List<WitnessCapsule> getWitnesses() {
    witsRead.lock();
    try {
      return this.wits;
    } finally {
      witsRead.unlock();
    }

  }

  // witness
  public WitnessCapsule getWitnesseByAddress(ByteString address) {
    witsRead.lock();
    try {
      final WitnessCapsule[] witnessCapsule = {null};
      this.wits.forEach(wit -> {
        if (Arrays.equals(address.toByteArray(), wit.getAddress().toByteArray())) {
          witnessCapsule[0] = wit;
          return;
        }
      });
      return witnessCapsule[0];
    } finally {
      witsRead.unlock();
    }

  }

  public void setWitnesses(List<WitnessCapsule> wits) {
    witsWrite.lock();
    this.wits = wits;
    witsWrite.unlock();
  }

  public void addWitness(final WitnessCapsule witnessCapsule) {
    witsWrite.lock();
    this.wits.add(witnessCapsule);
    witsWrite.unlock();
  }

  public void sortWitness() {
    witsWrite.lock();
    try {
      sortWitness(wits);
    } finally {
      witsWrite.unlock();
    }
  }


  /**
   * get slot at time.
   */
  public long getSlotAtTime(long when) {
    long firstSlotTime = getSlotTime(1);
    if (when < firstSlotTime) {
      return 0;
    }
    logger
        .debug("nextFirstSlotTime:[{}],when[{}]", new DateTime(firstSlotTime), new DateTime(when));
    return (when - firstSlotTime) / Manager.LOOP_INTERVAL + 1;
  }

  public BlockCapsule getGenesisBlock() {
    return manager.getGenesisBlock();
  }

  public BlockCapsule getHead() throws HeaderNotFound {
    return manager.getHead();
  }

  public boolean lastHeadBlockIsMaintenance() {
    return manager.lastHeadBlockIsMaintenance();
  }

  /**
   * get absolute Slot At Time
   */
  public long getAbSlotAtTime(long when) {
    return (when - getGenesisBlock().getTimeStamp()) / Manager.LOOP_INTERVAL;
  }

  /**
   * get slot time.
   */
  public long getSlotTime(long slotNum) {
    if (slotNum == 0) {
      return Time.getCurrentMillis();
    }
    long interval = Manager.LOOP_INTERVAL;

    if (manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 0) {
      return getGenesisBlock().getTimeStamp() + slotNum * interval;
    }

    if (lastHeadBlockIsMaintenance()) {
      slotNum += manager.getSkipSlotInMaintenance();
    }

    long headSlotTime = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();
    headSlotTime = headSlotTime
        - ((headSlotTime - getGenesisBlock().getTimeStamp()) % interval);

    return headSlotTime + interval * slotNum;
  }

  /**
   * validate witness schedule.
   */
  public boolean validateWitnessSchedule(BlockCapsule block) {

    ByteString witnessAddress = block.getInstance().getBlockHeader().getRawData()
        .getWitnessAddress();
    //to deal with other condition later
    if (manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() != 0 && manager
        .getDynamicPropertiesStore().getLatestBlockHeaderHash()
        .equals(block.getParentHash())) {
      long slot = getSlotAtTime(block.getTimeStamp());
      final ByteString scheduledWitness = getScheduledWitness(slot);
      if (!scheduledWitness.equals(witnessAddress)) {
        logger.warn(
            "Witness is out of order, scheduledWitness[{}],blockWitnessAddress[{}],blockTimeStamp[{}],slot[{}]",
            ByteArray.toHexString(scheduledWitness.toByteArray()),
            ByteArray.toHexString(witnessAddress.toByteArray()), new DateTime(block.getTimeStamp()),
            slot);
        return false;
      }
    }

    logger.debug("Validate witnessSchedule successfully,scheduledWitness:{}",
        ByteArray.toHexString(witnessAddress.toByteArray()));
    return true;
  }

  /**
   * get ScheduledWitness by slot.
   */
  public ByteString getScheduledWitness(final long slot) {

    final long currentSlot = getHeadSlot() + slot;

    if (currentSlot < 0) {
      throw new RuntimeException("currentSlot should be positive.");
    }
    final List<WitnessCapsule> currentShuffledWitnesses = this.getShuffledWitnessStates();
    if (CollectionUtils.isEmpty(currentShuffledWitnesses)) {
      throw new RuntimeException("ShuffledWitnesses is null.");
    }
    final int witnessIndex = (int) currentSlot % currentShuffledWitnesses.size();

    final ByteString scheduledWitness = currentShuffledWitnesses.get(witnessIndex).getAddress();
    //logger.info("scheduled_witness:" + scheduledWitness.toStringUtf8() + ",slot:" + currentSlot);

    return scheduledWitness;
  }

  public long getHeadSlot() {
    return (manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() - getGenesisBlock()
        .getTimeStamp())
        / Manager.LOOP_INTERVAL;
  }

  /**
   * shuffle witnesses
   */
  public void updateWitnessSchedule() {
    if (CollectionUtils.isEmpty(getWitnesses())) {
      throw new RuntimeException("Witnesses is empty");
    }

    List<String> currentWitsAddress = getWitnessStringList(getWitnesses());
    // TODO  what if the number of witness is not same in different slot.
    long num = manager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    long time = manager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp();

    if (num != 0 && num % getWitnesses().size() == 0) {
      logger.info("updateWitnessSchedule number:{},HeadBlockTimeStamp:{}", num, time);
      setShuffledWitnessStates(new RandomGenerator<WitnessCapsule>()
          .shuffle(getWitnesses(), time));

      logger.info(
          "updateWitnessSchedule,before:{} ", currentWitsAddress
              + ",\nafter:{} " + getWitnessStringList(getShuffledWitnessStates()));
    }
  }

  private Map<ByteString, Long> countVote(AccountStore accountStore) {

    final Map<ByteString, Long> countWitness = Maps.newHashMap();
    final List<AccountCapsule> accountList = accountStore.getAllAccounts();
    //logger.info("there is account List size is {}", accountList.size());
    accountList.forEach(account -> {
//      logger.info("there is account ,account address is {}",
//          account.createReadableString());

      Optional<Long> sum = account.getVotesList().stream().map(vote -> vote.getVoteCount())
          .reduce((a, b) -> a + b);
      if (sum.isPresent()) {
        if (sum.get() <= account.getShare()) {
          account.getVotesList().forEach(vote -> {
            //TODO validate witness //active_witness
            ByteString voteAddress = vote.getVoteAddress();
            long voteCount = vote.getVoteCount();
            if (countWitness.containsKey(voteAddress)) {
              countWitness.put(voteAddress, countWitness.get(voteAddress) + voteCount);
            } else {
              countWitness.put(voteAddress, voteCount);
            }
          });
        } else {
          logger.info(
              "account" + account.createReadableString() + ",share[" + account.getShare()
                  + "] > voteSum["
                  + sum.get() + "]");
        }
      }
    });
    return countWitness;
  }

  /**
   * update witness.
   */
  public void updateWitness() {
    WitnessStore witnessStore = manager.getWitnessStore();
    AccountStore accountStore = manager.getAccountStore();
    Map<ByteString, Long> countWitness = countVote(accountStore);

    //Only possible during the initialization phase
    if (countWitness.size() == 0) {
      logger.info("No vote, no change to witness.");
    } else {
      List<WitnessCapsule> currentWits = getWitnesses();

      final List<WitnessCapsule> witnessCapsuleList = Lists.newArrayList();
      witnessStore.getAllWitnesses().forEach(witnessCapsule -> {
        witnessCapsule.setVoteCount(0);
        witnessCapsule.setIsJobs(false);
        witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      });

      countWitness.forEach((address, voteCount) -> {
        final WitnessCapsule witnessCapsule = witnessStore.get(StringUtil.createDbKey(address));
        if (null == witnessCapsule) {
          logger
              .warn("witnessCapsule is null.address is {}",
                  StringUtil.createReadableString(address));
          return;
        }

        ByteString witnessAddress = witnessCapsule.getInstance().getAddress();
        AccountCapsule witnessAccountCapsule = accountStore
            .get(StringUtil.createDbKey(witnessAddress));
        if (witnessAccountCapsule == null) {
          logger.warn(
              "witnessAccount[" + StringUtil.createReadableString(witnessAddress) + "] not exists");
        } else {
          if (witnessAccountCapsule.getBalance() < WitnessCapsule.MIN_BALANCE) {
            logger.warn(
                "witnessAccount[" + StringUtil.createReadableString(witnessAddress)
                    + "] has balance["
                    + witnessAccountCapsule
                    .getBalance() + "] < MIN_BALANCE[" + WitnessCapsule.MIN_BALANCE + "]");
          } else {
            witnessCapsule.setVoteCount(witnessCapsule.getVoteCount() + voteCount);
            witnessCapsule.setIsJobs(false);
            witnessCapsuleList.add(witnessCapsule);
            witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
            logger.info("address is {}  ,countVote is {}", witnessCapsule.createReadableString(),
                witnessCapsule.getVoteCount());
          }
        }
      });

      sortWitness(witnessCapsuleList);
      if (witnessCapsuleList.size() > Manager.MAX_ACTIVE_WITNESS_NUM) {
        setWitnesses(witnessCapsuleList.subList(0, Manager.MAX_ACTIVE_WITNESS_NUM));
      } else {
        setWitnesses(witnessCapsuleList);
      }

      getWitnesses().forEach(witnessCapsule -> {
        witnessCapsule.setIsJobs(true);
        witnessStore.put(witnessCapsule.createDbKey(), witnessCapsule);
      });

      logger.info(
          "updateWitness,before:{} ",
          getWitnessStringList(currentWits) + ",\nafter:{} " + getWitnessStringList(
              getWitnesses()));
    }

  }

  public int calculateParticipationRate() {
    return manager.getDynamicPropertiesStore().calculateFilledSlotsCount();
  }

  private static List<String> getWitnessStringList(List<WitnessCapsule> witnessStates) {
    return witnessStates.stream()
        .map(witnessCapsule -> ByteArray.toHexString(witnessCapsule.getAddress().toByteArray()))
        .collect(Collectors.toList());
  }

  private static void sortWitness(List<WitnessCapsule> list) {
    list.sort((a, b) -> {
      if (b.getVoteCount() != a.getVoteCount()) {
        return (int) (b.getVoteCount() - a.getVoteCount());
      } else {
        return Long.compare(b.getAddress().hashCode(), a.getAddress().hashCode());
      }
    });
  }

}
