package org.tron.core.db;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;

@Slf4j
@Component
public class CommonDataBase extends TronDatabase<byte[]> {

  private static final String CURRENT_SR_LIST = "CURRENT_SR_LIST";
  private static final byte[] LATEST_PBFT_BLOCK_NUM = "LATEST_PBFT_BLOCK_NUM".getBytes();

  public CommonDataBase() {
    super("common-database");
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

  public List<String> getCurrentSrList(long cycle) {
    return Optional.ofNullable(get(buildKey(cycle)))
        .map(ByteArray::toStr)
        .map(str -> JSON.parseArray(str, String.class))
        .orElse(Lists.newArrayList());
  }

  public void saveCurrentSrList(long cycle, String currentSrList) {
    this.put(buildKey(cycle), ByteArray.fromString(currentSrList));
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

  private byte[] buildKey(long cycle) {
    return (cycle + "_" + CURRENT_SR_LIST).getBytes();
  }
}
