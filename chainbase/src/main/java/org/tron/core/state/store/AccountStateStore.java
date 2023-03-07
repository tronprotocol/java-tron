package org.tron.core.state.store;

import lombok.Getter;
import org.rocksdb.DirectComparator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.BadItemException;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.store.AccountStore;

import java.util.Iterator;
import java.util.Map;

public class AccountStateStore extends AccountStore implements StateStore {

  private WorldStateQueryInstance worldStateQueryInstance;
  @Getter
  private boolean init;

  @Override
  public void init(WorldStateQueryInstance worldStateQueryInstance) {
    this.worldStateQueryInstance = worldStateQueryInstance;
    this.init = true;
  }

  //****  Override Operation For StateDB

  @Override
  public String getDbName() {
    return worldStateQueryInstance.getRootHash().toHexString();
  }

  @Override
  public AccountCapsule get(byte[] key) {
    return getFromRoot(key);
  }

  @Override
  public AccountCapsule getFromRoot(byte[] key) {
    return getUnchecked(key);

  }

  @Override
  public AccountCapsule getUnchecked(byte[] key) {
    throwIfNotInit();
    return worldStateQueryInstance.getAccount(key);
  }

  @Override
  public boolean has(byte[] key) {
    return get(key) != null;
  }

  @Override
  public void close() {
  }

  @Override
  public void reset() {
  }

  //****  Override Operation For StateDB

  //****  Unsupported Operation For StateDB

  public static void setAccount(com.typesafe.config.Config config) {
    throw new UnsupportedOperationException();
  }

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    throw new UnsupportedOperationException();
  }

  protected DirectComparator getDirectComparator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void put(byte[] key, AccountCapsule item) {
    throwIfError();
  }

  @Override
  public void delete(byte[] key) {
    throwIfError();
  }

  @Override
  public AccountCapsule of(byte[] value) throws BadItemException {
    throwIfError();
    return null;
  }

  @Override
  public boolean isNotEmpty() {
    throwIfError();
    return false;
  }

  @Override
  public Iterator<Map.Entry<byte[], AccountCapsule>> iterator() {
    throwIfError();
    return null;
  }

  public long size() {
    throwIfError();
    return 0;
  }

  public void setCursor(Chainbase.Cursor cursor) {
    throwIfError();
  }

  public Map<WrappedByteArray, AccountCapsule> prefixQuery(byte[] key) {
    throwIfError();
    return null;
  }

  //****  Unsupported Operation For StateDB
}
