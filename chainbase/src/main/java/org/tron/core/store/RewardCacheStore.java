package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.iterator.DBIterator;

@Slf4j(topic = "DB")
@Component
public class RewardCacheStore extends TronDatabase<byte[]> {

  @Autowired
  private RewardCacheStore(@Value("reward-cache") String dbName) {
    super(dbName);
  }

  @Override
  public byte[] get(byte[] key) {
    return dbSource.getData(key);
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
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  public void putReward(byte[] key, long reward) {
    put(key, ByteArray.fromLong(reward));
  }

  public long getReward(byte[] key) {
    byte[] value = get(key);
    return value == null ? -1 : ByteArray.toLong(value);
  }

  @Override
  public DBIterator iterator() {
    return ((DBIterator) dbSource.iterator());
  }
}
