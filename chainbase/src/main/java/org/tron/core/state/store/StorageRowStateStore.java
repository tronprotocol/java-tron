package org.tron.core.state.store;

import org.rocksdb.AbstractComparator;
import org.tron.core.capsule.StorageRowCapsule;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.state.WorldStateQueryInstance;
import org.tron.core.store.StorageRowStore;

import java.util.Iterator;
import java.util.Map;

public class StorageRowStateStore extends StorageRowStore implements StateStore {

  private WorldStateQueryInstance worldStateQueryInstance;

  public StorageRowStateStore(WorldStateQueryInstance worldStateQueryInstance) {
    this.worldStateQueryInstance = worldStateQueryInstance;
  }

  //****  Override Operation For StateDB

  @Override
  public String getDbName() {
    return worldStateQueryInstance.getRootHash().toHexString();
  }

  @Override
  public StorageRowCapsule get(byte[] key) {
    return getFromRoot(key);
  }

  @Override
  public StorageRowCapsule getFromRoot(byte[] key) {
    return getUnchecked(key);

  }

  @Override
  public StorageRowCapsule getUnchecked(byte[] key) {
    return worldStateQueryInstance.getStorageRow(key);
  }

  @Override
  public boolean has(byte[] key) {
    return getUnchecked(key).getData() != null;
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
  public void put(byte[] key, StorageRowCapsule item) {
    throwIfError();
  }

  @Override
  public void delete(byte[] key) {
    throwIfError();
  }

  @Override
  public StorageRowCapsule of(byte[] value) {
    throwIfError();
    return null;
  }

  @Override
  public boolean isNotEmpty() {
    throwIfError();
    return false;
  }

  @Override
  public Iterator<Map.Entry<byte[], StorageRowCapsule>> iterator() {
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

  public Map<WrappedByteArray, StorageRowCapsule> prefixQuery(byte[] key) {
    throwIfError();
    return null;
  }

  //****  Unsupported Operation For StateDB
}
