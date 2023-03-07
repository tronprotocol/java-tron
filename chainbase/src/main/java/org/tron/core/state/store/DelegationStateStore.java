package org.tron.core.state.store;

import lombok.Getter;
import org.rocksdb.DirectComparator;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.BadItemException;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.store.DelegationStore;

import java.util.Iterator;
import java.util.Map;

public class DelegationStateStore extends DelegationStore implements StateStore {

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
  public BytesCapsule get(byte[] key) {
    return getFromRoot(key);
  }

  @Override
  public BytesCapsule getFromRoot(byte[] key) {
    return getUnchecked(key);

  }

  @Override
  public BytesCapsule getUnchecked(byte[] key) {
    throwIfNotInit();
    return worldStateQueryInstance.getDelegation(key);
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

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    throw new UnsupportedOperationException();
  }

  protected DirectComparator getDirectComparator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void put(byte[] key, BytesCapsule item) {
    throwIfError();
  }

  @Override
  public void delete(byte[] key) {
    throwIfError();
  }

  @Override
  public BytesCapsule of(byte[] value) throws BadItemException {
    throwIfError();
    return null;
  }

  @Override
  public boolean isNotEmpty() {
    throwIfError();
    return false;
  }

  @Override
  public Iterator<Map.Entry<byte[], BytesCapsule>> iterator() {
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

  public Map<WrappedByteArray, BytesCapsule> prefixQuery(byte[] key) {
    throwIfError();
    return null;
  }

  //****  Unsupported Operation For StateDB
}
