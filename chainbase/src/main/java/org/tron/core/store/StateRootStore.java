package org.tron.core.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.db.TronDatabase;
import org.tron.core.db.common.iterator.DBIterator;

@Slf4j(topic = "DB")
@Component
public class StateRootStore extends TronDatabase<byte[]> {

  @Autowired
  private StateRootStore(@Value("state-root") String dbName) {
    super(dbName);
  }

  @Override
  public byte[] get(byte[] key) {
    return dbSource.getData(key);
  }

  public byte[] get(long key) {
    return dbSource.getData(ByteArray.fromLong(key));
  }

  @Override
  public void put(byte[] key, byte[] item) {
    dbSource.putData(key, item);
  }

  public void put(long key, Sha256Hash root) {
    logger.info("block: {}, stateRoot: {}", key, root);
    this.put(ByteArray.fromLong(key), root.getBytes());
  }

  @Override
  public void delete(byte[] key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean has(byte[] key) {
    return dbSource.getData(key) != null;
  }

  @Override
  public DBIterator iterator() {
    return ((DBIterator) dbSource.iterator());
  }
}
