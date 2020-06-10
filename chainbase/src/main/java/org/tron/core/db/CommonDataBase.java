package org.tron.core.db;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.protos.Protocol;

@Slf4j
@Component
public class CommonDataBase extends TronDatabase<byte[]> {

  private static final byte[] LATEST_PBFT_BLOCK_NUM = "LATEST_PBFT_BLOCK_NUM".getBytes();
  private static final byte[] LATEST_SYNC_BLOCK_NUM = "LATEST_SYNC_BLOCK_NUM".getBytes();
  private static final byte[] FIRST_PBFT_BLOCK_NUM = "FIRST_PBFT_BLOCK_NUM".getBytes();
  private static final byte[] LATEST_PBFT_BLOCK_HASH = "LATEST_PBFT_BLOCK_HASH".getBytes();
  private static final byte[] NEXT_EPOCH = "NEXT_EPOCH".getBytes();
  private static final byte[] CURRENT_EPOCH = "CURRENT_EPOCH".getBytes();

  public CommonDataBase() {
    super("common-database");
  }

  public CommonDataBase(String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, byte[] item) {
    dbSource.putData(key, item);
  }

  @Override
  public void delete(byte[] key) {
    dbSource.deleteData(key);
  }

  @Override
  public byte[] get(byte[] key) {
    return dbSource.getData(key);
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  public void saveLatestPbftBlockNum(long number) {
    if (number <= getLatestPbftBlockNum()) {
      logger.warn("pbft number {} <= latest number {}", number, getLatestPbftBlockNum());
      return;
    }
    this.put(LATEST_PBFT_BLOCK_NUM, ByteArray.fromLong(number));
  }

  public long getLatestPbftBlockNum() {
    return Optional.ofNullable(get(LATEST_PBFT_BLOCK_NUM))
        .map(ByteArray::toLong)
        .orElse(0L);
  }

  public long getLatestPBFTBlockNum(String chainId) {
    return Optional.ofNullable(get(buildKey(LATEST_PBFT_BLOCK_NUM, chainId)))
        .map(ByteArray::toLong)
        .orElse(0L);
  }

  public void saveLatestPBFTBlockNum(String chainId, long number) {
    if (number <= getLatestPBFTBlockNum(chainId)) {
      logger.warn("chainId: {}, pbft number {} <= latest number {}", chainId, number,
          getLatestPBFTBlockNum(chainId));
      return;
    }
    this.put(buildKey(LATEST_PBFT_BLOCK_NUM, chainId), Longs.toByteArray(number));
  }

  public long getLatestSyncBlockNum(String chainId) {
    return Optional.ofNullable(get(buildKey(LATEST_SYNC_BLOCK_NUM, chainId)))
        .map(ByteArray::toLong)
        .orElse(0L);
  }

  public void saveLatestSyncBlockNum(String chainId, long number) {
    if (number <= getLatestSyncBlockNum(chainId)) {
      logger.warn("chainId: {}, sync number {} <= latest number {}",
          chainId, number, getLatestSyncBlockNum(chainId));
      return;
    }
    this.put(buildKey(LATEST_SYNC_BLOCK_NUM, chainId), Longs.toByteArray(number));
  }

  public long getFirstPBFTBlockNum(String chainId) {
    return Optional.ofNullable(get(buildKey(FIRST_PBFT_BLOCK_NUM, chainId)))
        .map(ByteArray::toLong)
        .orElse(0L);
  }

  public void saveFirstPBFTBlockNum(String chainId, long number) {
    if (number <= getFirstPBFTBlockNum(chainId)) {
      logger.warn("chainId: {}, pbft number {} <= latest number {}",
          chainId, number, getFirstPBFTBlockNum(chainId));
      return;
    }
    this.put(buildKey(FIRST_PBFT_BLOCK_NUM, chainId), Longs.toByteArray(number));
  }

  private byte[] buildKey(byte[] prefix, String chainId) {
    return Bytes.concat(prefix, chainId.getBytes());
  }

  public void saveLatestPbftBlockHash(byte[] data) {
    this.put(LATEST_PBFT_BLOCK_HASH, data);
  }

  public Sha256Hash getLatestPbftBlockHash() {
    byte[] date = this.get(LATEST_PBFT_BLOCK_HASH);

    if (ByteUtil.isNullOrZeroArray(date)) {
      return null;
    }
    return Sha256Hash.wrap(date);
  }

  public Protocol.SRL getSRL(String chainId, long epoch) {
    byte[] value = get(buildKey(ByteArray.fromLong(epoch), chainId));
    if (ByteUtil.isNullOrZeroArray(value)) {
      return null;
    } else {
      try {
        return Protocol.SRL.parseFrom(value);
      } catch (InvalidProtocolBufferException e) {
        logger.error("", e);
        return null;
      }
    }
  }

  public void saveSRL(String chainId, long epoch, Protocol.SRL srl) {
    Protocol.SRL value = getSRL(chainId, epoch);
    if (value == null) {
      put(buildKey(ByteArray.fromLong(epoch), chainId), srl.toByteArray());
    }
  }

  public long getNextEpoch(String chainId) {
    byte[] value = get(buildKey(NEXT_EPOCH, chainId));
    if (value == null || value.length == 0) {
      return 1L;
    } else {
      return ByteArray.toLong(value);
    }
  }

  public void saveNextEpoch(String chainId, long nextEpoch) {
    this.put(buildKey(NEXT_EPOCH, chainId), ByteArray.fromLong(nextEpoch));
  }

  public void updateNextEpoch(String chainId, long blockTime) {
    long maintenanceTimeInterval = CommonParameter.getInstance().getMaintenanceTimeInterval();

    long currentEpoch = getNextEpoch(chainId);
    long round = (blockTime - currentEpoch) / maintenanceTimeInterval;
    long nextEpoch = currentEpoch + (round + 1) * maintenanceTimeInterval;
    saveNextEpoch(chainId, nextEpoch);
    saveCurrentEpoch(chainId, currentEpoch);

    logger.info(
        "do update nextEpoch, chainId:{}, currentEpoch:{}, blockTime:{}, nextEpoch:{}",
        chainId,
        new DateTime(currentEpoch), new DateTime(blockTime),
        new DateTime(nextEpoch)
    );
  }

  public long getCurrentEpoch(String chainId) {
    byte[] value = get(buildKey(CURRENT_EPOCH, chainId));
    if (value == null || value.length == 0) {
      return 1L;
    } else {
      return ByteArray.toLong(value);
    }
  }

  public void saveCurrentEpoch(String chainId, long currentEpoch) {
    this.put(buildKey(NEXT_EPOCH, chainId), ByteArray.fromLong(currentEpoch));
  }

}
