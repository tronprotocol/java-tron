package org.tron.core.state.store;

import org.rocksdb.AbstractComparator;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.store.DelegationStore;

import java.util.Iterator;
import java.util.Map;

public class DelegationStateStore extends DelegationStore implements StateStore {

  private WorldStateQueryInstance worldStateQueryInstance;

  public DelegationStateStore(WorldStateQueryInstance worldStateQueryInstance) {
    this.worldStateQueryInstance = worldStateQueryInstance;
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
    return worldStateQueryInstance.getDelegation(key);
  }

  @Override
  public boolean has(byte[] key) {
    return getUnchecked(key) != null;
  }

  @Override
  public void close() {
    this.worldStateQueryInstance = null;
  }

  @Override
  public void reset() {
  }

  //****  Override Operation For StateDB

  //****  Unsupported Operation For StateDB

  protected org.iq80.leveldb.Options getOptionsByDbNameForLevelDB(String dbName) {
    throw new UnsupportedOperationException();
  }

  protected AbstractComparator getDirectComparator() {
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
  public BytesCapsule of(byte[] value) {
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
