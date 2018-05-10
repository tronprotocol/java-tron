package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.args.Args;

@Slf4j
@Component
public class DynamicPropertiesStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final byte[] MAINTENANCE_TIME_INTERVAL = "MAINTENANCE_TIME_INTERVAL".getBytes();
  private static final long MAINTENANCE_SKIP_SLOTS = 2;

  private static final byte[] VOTE_REWARD_RATE = "VOTE_REWARD_RATE".getBytes(); // percent
  private static final byte[] SINGLE_REPEAT = "SINGLE_REPEAT".getBytes();

  private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp"
      .getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();
  private static final byte[] STATE_FLAG = "state_flag"
      .getBytes(); // 1 : is maintenance, 0 : is not maintenance
  private static final byte[] LATEST_SOLIDIFIED_BLOCK_NUM = "LATEST_SOLIDIFIED_BLOCK_NUM"
      .getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS = "BLOCK_FILLED_SLOTS".getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS_INDEX = "BLOCK_FILLED_SLOTS_INDEX".getBytes();

  private static final byte[] NEXT_MAINTENANCE_TIME = "NEXT_MAINTENANCE_TIME".getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS_NUMBER = "BLOCK_FILLED_SLOTS_NUMBER".getBytes();

  private static final byte[] MAX_VOTE_NUMBER = "MAX_VOTE_NUMBER".getBytes();

  private static final byte[] MAX_FROZEN_NUMBER = "MAX_FROZEN_NUMBER".getBytes();

  private static final byte[] MAX_FROZEN_TIME = "MAX_FROZEN_TIME".getBytes();

  private static final byte[] MIN_FROZEN_TIME = "MIN_FROZEN_TIME".getBytes();

  private static final byte[] WITNESS_ALLOWANCE_FROZEN_TIME = "WITNESS_ALLOWANCE_FROZEN_TIME"
      .getBytes();

  private static final byte[] BANDWIDTH_PER_TRANSACTION = "BANDWIDTH_PER_TRANSACTION".getBytes();

  private static final byte[] BANDWIDTH_PER_COINDAY = "BANDWIDTH_PER_COINDAY".getBytes();

  private static final byte[] ACCOUNT_UPGRADE_COST = "ACCOUNT_UPGRADE_COST".getBytes();
  // 1_000_000L
  private static final byte[] NON_EXISTENT_ACCOUNT_TRANSFER_MIN = "NON_EXISTENT_ACCOUNT_TRANSFER_MIN"
      .getBytes();

  private static final byte[] OPERATING_TIME_INTERVAL = "OPERATING_TIME_INTERVAL".getBytes();

  @Autowired
  private DynamicPropertiesStore(@Qualifier("properties") String dbName) {
    super(dbName);
    try {
      this.getMaintenanceTimeInterval();
    } catch (IllegalArgumentException e) {
      this.saveMaintenanceTimeInterval(Args.getInstance().getMaintenanceTimeInterval());
    }

    try {
      this.getVoteRewardRate();
    } catch (IllegalArgumentException e) {
      this.saveVoteRewardRate(0);
    }

    try {
      this.getSingleRepeat();
    } catch (IllegalArgumentException e) {
      this.saveSingleRepeat(1);
    }

    try {
      this.getLatestBlockHeaderTimestamp();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderTimestamp(0);
    }

    try {
      this.getLatestBlockHeaderNumber();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderNumber(0);
    }

    try {
      this.getLatestBlockHeaderHash();
    } catch (IllegalArgumentException e) {
      this.saveLatestBlockHeaderHash(ByteString.copyFrom(ByteArray.fromHexString("00")));
    }

    try {
      this.getStateFlag();
    } catch (IllegalArgumentException e) {
      this.saveStateFlag(0);
    }

    try {
      this.getLatestSolidifiedBlockNum();
    } catch (IllegalArgumentException e) {
      this.saveLatestSolidifiedBlockNum(0);
    }

    try {
      this.getBlockFilledSlotsIndex();
    } catch (IllegalArgumentException e) {
      this.saveBlockFilledSlotsIndex(0);
    }

    try {
      this.getMaxVoteNumber();
    } catch (IllegalArgumentException e) {
      this.saveMaxVoteNumber(30);
    }

    try {
      this.getMaxFrozenNumber();
    } catch (IllegalArgumentException e) {
      this.saveMaxFrozenNumber(1);
    }

    try {
      this.getMaxFrozenTime();
    } catch (IllegalArgumentException e) {
      this.saveMaxFrozenTime(3);
    }

    try {
      this.getMinFrozenTime();
    } catch (IllegalArgumentException e) {
      this.saveMinFrozenTime(3);
    }

    try {
      this.getWitnessAllowanceFrozenTime();
    } catch (IllegalArgumentException e) {
      this.saveWitnessAllowanceFrozenTime(1);
    }

    try {
      this.getBandwidthPerTransaction();
    } catch (IllegalArgumentException e) {
      this.saveBandwidthPerTransaction(100_000);
    }

    try {
      this.getBandwidthPerCoinday();
    } catch (IllegalArgumentException e) {
      this.saveBandwidthPerCoinday(1);
    }

    try {
      this.getAccountUpgradeCost();
    } catch (IllegalArgumentException e) {
      this.saveAccountUpgradeCost(100_000_000_000L);
    }

    try {
      this.getNonExistentAccountTransferMin();
    } catch (IllegalArgumentException e) {
      this.saveNonExistentAccountTransferLimit(1_000_000L);
    }


    try {
      this.getOperatingTimeInterval();
    } catch (IllegalArgumentException e) {
      this.saveOperatingTimeInterval(10_000L);
    }


    try {
      this.getBlockFilledSlotsNumber();
    } catch (IllegalArgumentException e) {
      this.saveBlockFilledSlotsNumber(128);
    }

    try {
      this.getBlockFilledSlots();
    } catch (IllegalArgumentException e) {
      int[] blockFilledSlots = new int[getBlockFilledSlotsNumber()];
      Arrays.fill(blockFilledSlots, 1);
      this.saveBlockFilledSlots(blockFilledSlots);
    }

    try {
      this.getNextMaintenanceTime();
    } catch (IllegalArgumentException e) {
      this.saveNextMaintenanceTime(
          Long.parseLong(Args.getInstance().getGenesisBlock().getTimestamp()));
    }

  }

  @Override
  public BytesCapsule get(byte[] key) {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  private static DynamicPropertiesStore instance;

  public static void destroy() {
    instance = null;
  }

  /**
   * create fun.
   *
   * @param dbName the name of database
   */

  public static DynamicPropertiesStore create(String dbName) {
    if (instance == null) {
      synchronized (DynamicPropertiesStore.class) {
        if (instance == null) {
          instance = new DynamicPropertiesStore(dbName);
        }
      }
    }
    return instance;
  }

  public String intArrayToString(int[] a) {
    StringBuilder sb = new StringBuilder();
    for (int i : a) {
      sb.append(i);
    }
    return sb.toString();
  }

  public int[] stringToIntArray(String s) {
    int length = s.length();
    int[] result = new int[length];
    for (int i = 0; i < length; ++i) {
      result[i] = Integer.parseInt(s.substring(i, i + 1));
    }
    return result;
  }

  public void saveMaintenanceTimeInterval(long maintenanceTimeInterval) {
    logger.debug("MaintenanceTimeInterval:" + maintenanceTimeInterval);
    this.put(MAINTENANCE_TIME_INTERVAL,
        new BytesCapsule(ByteArray.fromObject(maintenanceTimeInterval)));
  }

  public long getMaintenanceTimeInterval() {
    return Optional.ofNullable(this.dbSource.getData(MAINTENANCE_TIME_INTERVAL))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAINTENANCE_TIME_INTERVAL"));
  }

  public void saveVoteRewardRate(double voteRewardRate) {
    logger.debug("VoteRewardRate:" + voteRewardRate);
    this.put(VOTE_REWARD_RATE,
        new BytesCapsule(ByteArray.fromString(Double.toString(voteRewardRate))));
  }

  public double getVoteRewardRate() {
    return Optional.ofNullable(this.dbSource.getData(VOTE_REWARD_RATE))
        .map(ByteArray::toStr)
        .map(Double::parseDouble)
        .orElseThrow(
            () -> new IllegalArgumentException("not found VOTE_REWARD_RATE"));
  }

  public void saveSingleRepeat(int singleRepeat) {
    logger.debug("SingleRepeat:" + singleRepeat);
    this.put(SINGLE_REPEAT,
        new BytesCapsule(ByteArray.fromInt(singleRepeat)));
  }

  public int getSingleRepeat() {
    return Optional.ofNullable(this.dbSource.getData(SINGLE_REPEAT))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found SINGLE_REPEAT"));
  }

  public void saveBlockFilledSlotsIndex(int blockFilledSlotsIndex) {
    logger.debug("blockFilledSlotsIndex:" + blockFilledSlotsIndex);
    this.put(BLOCK_FILLED_SLOTS_INDEX,
        new BytesCapsule(ByteArray.fromInt(blockFilledSlotsIndex)));
  }

  public int getBlockFilledSlotsIndex() {
    return Optional.ofNullable(this.dbSource.getData(BLOCK_FILLED_SLOTS_INDEX))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found BLOCK_FILLED_SLOTS_INDEX"));
  }

  public void saveMaxFrozenNumber(int maxFrozenNumber) {
    logger.debug("MAX_FROZEN_NUMBER:" + maxFrozenNumber);
    this.put(MAX_FROZEN_NUMBER,
        new BytesCapsule(ByteArray.fromInt(maxFrozenNumber)));
  }

  public int getMaxFrozenNumber() {
    return Optional.ofNullable(this.dbSource.getData(MAX_FROZEN_NUMBER))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAX_FROZEN_NUMBER"));
  }

  public void saveMaxFrozenTime(int maxFrozenTime) {
    logger.debug("MAX_FROZEN_NUMBER:" + maxFrozenTime);
    this.put(MAX_FROZEN_TIME,
        new BytesCapsule(ByteArray.fromInt(maxFrozenTime)));
  }

  public int getMaxFrozenTime() {
    return Optional.ofNullable(this.dbSource.getData(MAX_FROZEN_TIME))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAX_FROZEN_TIME"));
  }

  public void saveMinFrozenTime(int minFrozenTime) {
    logger.debug("MIN_FROZEN_NUMBER:" + minFrozenTime);
    this.put(MIN_FROZEN_TIME,
        new BytesCapsule(ByteArray.fromInt(minFrozenTime)));
  }

  public int getMinFrozenTime() {
    return Optional.ofNullable(this.dbSource.getData(MIN_FROZEN_TIME))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MIN_FROZEN_TIME"));
  }

  public void saveWitnessAllowanceFrozenTime(int witnessAllowanceFrozenTime) {
    logger.debug("WITNESS_ALLOWANCE_FROZEN_TIME:" + witnessAllowanceFrozenTime);
    this.put(WITNESS_ALLOWANCE_FROZEN_TIME,
        new BytesCapsule(ByteArray.fromInt(witnessAllowanceFrozenTime)));
  }

  public int getWitnessAllowanceFrozenTime() {
    return Optional.ofNullable(this.dbSource.getData(WITNESS_ALLOWANCE_FROZEN_TIME))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found WITNESS_ALLOWANCE_FROZEN_TIME"));
  }

  public void saveBandwidthPerTransaction(int bandwidthPerTransaction) {
    logger.debug("BANDWIDTH_PER_TRANSACTION:" + bandwidthPerTransaction);
    this.put(BANDWIDTH_PER_TRANSACTION,
        new BytesCapsule(ByteArray.fromInt(bandwidthPerTransaction)));
  }

  public int getBandwidthPerTransaction() {
    return Optional.ofNullable(this.dbSource.getData(BANDWIDTH_PER_TRANSACTION))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found BANDWIDTH_PER_TRANSACTION"));
  }

  public void saveBandwidthPerCoinday(long bandwidthPerCoinday) {
    logger.debug("BANDWIDTH_PER_COINDAY:" + bandwidthPerCoinday);
    this.put(BANDWIDTH_PER_COINDAY,
        new BytesCapsule(ByteArray.fromLong(bandwidthPerCoinday)));
  }

  public long getBandwidthPerCoinday() {
    return Optional.ofNullable(this.dbSource.getData(BANDWIDTH_PER_COINDAY))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found BANDWIDTH_PER_COINDAY"));
  }

  public void saveAccountUpgradeCost(long accountUpgradeCost) {
    logger.debug("ACCOUNT_UPGRADE_COST:" + accountUpgradeCost);
    this.put(ACCOUNT_UPGRADE_COST,
        new BytesCapsule(ByteArray.fromLong(accountUpgradeCost)));
  }

  public long getAccountUpgradeCost() {
    return Optional.ofNullable(this.dbSource.getData(ACCOUNT_UPGRADE_COST))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ACCOUNT_UPGRADE_COST"));
  }

  public void saveNonExistentAccountTransferLimit(long limit) {
    logger.debug("NON_EXISTENT_ACCOUNT_TRANSFER_MIN:" + limit);
    this.put(NON_EXISTENT_ACCOUNT_TRANSFER_MIN,
        new BytesCapsule(ByteArray.fromLong(limit)));
  }

  public long getNonExistentAccountTransferMin() {
    return Optional.ofNullable(this.dbSource.getData(NON_EXISTENT_ACCOUNT_TRANSFER_MIN))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found NON_EXISTENT_ACCOUNT_TRANSFER_MIN"));
  }


  public void saveOperatingTimeInterval(long time) {
    logger.debug("NON_EXISTENT_ACCOUNT_TRANSFER_MIN:" + time);
    this.put(OPERATING_TIME_INTERVAL,
        new BytesCapsule(ByteArray.fromLong(time)));
  }

  public long getOperatingTimeInterval() {
    return Optional.ofNullable(this.dbSource.getData(OPERATING_TIME_INTERVAL))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found OPERATING_TIME_INTERVAL"));
  }



  public void saveBlockFilledSlots(int[] blockFilledSlots) {
    logger.debug("blockFilledSlots:" + intArrayToString(blockFilledSlots));
    this.put(BLOCK_FILLED_SLOTS,
        new BytesCapsule(ByteArray.fromString(intArrayToString(blockFilledSlots))));
  }

  public int[] getBlockFilledSlots() {
    return Optional.ofNullable(this.dbSource.getData(BLOCK_FILLED_SLOTS))
        .map(ByteArray::toStr)
        .map(this::stringToIntArray)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM timestamp"));
  }

  public int getBlockFilledSlotsNumber() {
    return Optional.ofNullable(this.dbSource.getData(BLOCK_FILLED_SLOTS_NUMBER))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found BLOCK_FILLED_SLOTS_NUMBER"));
  }

  public void saveBlockFilledSlotsNumber(int blockFilledSlotsNumber) {
    logger.debug("blockFilledSlotsNumber:" + blockFilledSlotsNumber);
    this.put(BLOCK_FILLED_SLOTS_NUMBER,
        new BytesCapsule(ByteArray.fromInt(blockFilledSlotsNumber)));
  }

  public int getMaxVoteNumber() {
    return Optional.ofNullable(this.dbSource.getData(MAX_VOTE_NUMBER))
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAX_VOTE_NUMBER"));
  }

  public void saveMaxVoteNumber(int maxVoteNumber) {
    logger.debug("MAX_VOTE_NUMBER:" + maxVoteNumber);
    this.put(MAX_VOTE_NUMBER,
        new BytesCapsule(ByteArray.fromInt(maxVoteNumber)));
  }

  public void applyBlock(boolean fillBlock) {
    int[] blockFilledSlots = getBlockFilledSlots();
    int blockFilledSlotsIndex = getBlockFilledSlotsIndex();
    blockFilledSlots[blockFilledSlotsIndex] = fillBlock ? 1 : 0;
    saveBlockFilledSlotsIndex((blockFilledSlotsIndex + 1) % getBlockFilledSlotsNumber());
    saveBlockFilledSlots(blockFilledSlots);
  }

  public int calculateFilledSlotsCount() {
    int[] blockFilledSlots = getBlockFilledSlots();
    return 100 * IntStream.of(blockFilledSlots).sum() / getBlockFilledSlotsNumber();
  }

  public void saveLatestSolidifiedBlockNum(long number) {
    this.put(LATEST_SOLIDIFIED_BLOCK_NUM, new BytesCapsule(ByteArray.fromLong(number)));
  }


  public long getLatestSolidifiedBlockNum() {
    return Optional.ofNullable(this.dbSource.getData(LATEST_SOLIDIFIED_BLOCK_NUM))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM timestamp"));
    //return ByteArray.toLong(this.dbSource.getData(this.SOLIDIFIED_THRESHOLD));
  }

  /**
   * get timestamp of creating global latest block.
   */
  public long getLatestBlockHeaderTimestamp() {
    return Optional.ofNullable(this.dbSource.getData(LATEST_BLOCK_HEADER_TIMESTAMP))
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found latest block header timestamp"));
  }

  /**
   * get number of global latest block.
   */
  public long getLatestBlockHeaderNumber() {
    return Optional.ofNullable(this.dbSource.getData(LATEST_BLOCK_HEADER_NUMBER))
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found latest block header number"));
  }

  public int getStateFlag() {
    return Optional.ofNullable(this.dbSource.getData(STATE_FLAG))
        .map(ByteArray::toInt)
        .orElseThrow(() -> new IllegalArgumentException("not found maintenance flag"));
  }

  /**
   * get id of global latest block.
   */

  public Sha256Hash getLatestBlockHeaderHash() {

    byte[] blockHash = Optional.ofNullable(this.dbSource.getData(LATEST_BLOCK_HEADER_HASH))
        .orElseThrow(() -> new IllegalArgumentException("not found block hash"));
    return Sha256Hash.wrap(blockHash);
  }

  /**
   * save timestamp of creating global latest block.
   */
  public void saveLatestBlockHeaderTimestamp(long t) {
    logger.info("update latest block header timestamp = {}", t);
    this.put(LATEST_BLOCK_HEADER_TIMESTAMP, new BytesCapsule(ByteArray.fromLong(t)));
  }

  /**
   * save number of global latest block.
   */
  public void saveLatestBlockHeaderNumber(long n) {
    logger.info("update latest block header number = {}", n);
    this.put(LATEST_BLOCK_HEADER_NUMBER, new BytesCapsule(ByteArray.fromLong(n)));
  }

  /**
   * save id of global latest block.
   */
  public void saveLatestBlockHeaderHash(ByteString h) {
    logger.info("update latest block header id = {}", ByteArray.toHexString(h.toByteArray()));
    this.put(LATEST_BLOCK_HEADER_HASH, new BytesCapsule(h.toByteArray()));
  }

  public void saveStateFlag(int n) {
    logger.info("update state flag = {}", n);
    this.put(STATE_FLAG, new BytesCapsule(ByteArray.fromInt(n)));
  }


  public long getNextMaintenanceTime() {
    return Optional.ofNullable(this.dbSource.getData(NEXT_MAINTENANCE_TIME))
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found NEXT_MAINTENANCE_TIME"));
  }

  public long getMaintenanceSkipSlots() {
    return MAINTENANCE_SKIP_SLOTS;
  }

  private void saveNextMaintenanceTime(long nextMaintenanceTime) {
    this.put(NEXT_MAINTENANCE_TIME,
        new BytesCapsule(ByteArray.fromLong(nextMaintenanceTime)));
  }


  public void updateNextMaintenanceTime(long blockTime) {

    long currentMaintenanceTime = getNextMaintenanceTime();
    long round = (blockTime - currentMaintenanceTime) / getMaintenanceTimeInterval();
    long nextMaintenanceTime = currentMaintenanceTime + (round + 1) * getMaintenanceTimeInterval();
    saveNextMaintenanceTime(nextMaintenanceTime);

    logger.info(
        "do update nextMaintenanceTime,currentMaintenanceTime:{}, blockTime:{},nextMaintenanceTime:{}",
        new DateTime(currentMaintenanceTime), new DateTime(blockTime),
        new DateTime(nextMaintenanceTime)
    );
  }

}
