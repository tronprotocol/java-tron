package org.tron.core.db;

import java.util.Objects;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.core.capsule.ProtoCapsule;
import org.tron.core.db.AbstractRevokingStore.RevokingTuple;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.core.RevokingDBWithCachingOldValue;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

@Slf4j
public abstract class TronStoreWithRevoking<T extends ProtoCapsule> extends TronDatabase<T> {

  private IRevokingDB revokingDB;

  protected TronStoreWithRevoking(String dbName) {
    this.revokingDB = new RevokingDBWithCachingOldValue(dbName);
  }

  @Override
  public void put(byte[] key, T item) {
    if (Objects.isNull(key) || Objects.isNull(item)) {
      return;
    }

    revokingDB.put(key, item.getData());
  }

  @Override
  public void delete(byte[] key) {
    revokingDB.delete(key);
  }

  @Override
  public T get(byte[] key) throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException {
    byte[] value = revokingDB.get(key);
    return of(value);
  }

  public abstract T of(byte[] key) throws BadItemException;

  @Override
  public boolean has(byte[] key) {
    return revokingDB.has(key);
  }

  @Override
  public void close() {
    revokingDB.close();
  }

  @Override
  public void reset() {
    revokingDB.reset();
  }
}
